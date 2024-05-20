package com.QueenMedusa


import com.fasterxml.jackson.annotation.JsonProperty
import com.lustyflix.streamverse.*
import com.lustyflix.streamverse.LoadResponse.Companion.addDuration
import com.lustyflix.streamverse.utils.*
import org.jsoup.Jsoup
import com.lustyflix.streamverse.utils.M3u8Helper.Companion.generateM3u8
import com.lustyflix.streamverse.syncproviders.SyncIdName
import kotlinx.coroutines.delay
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.net.URLEncoder
import com.lustyflix.streamverse.LoadResponse.Companion.addAniListId
import com.lustyflix.streamverse.utils.JsUnpacker
import com.lustyflix.streamverse.utils.Qualities
//import android.util.Log //(only required for debugging)

class AniwaveProvider : MainAPI() {
    override var mainUrl = AniwaveProviderPlugin.currentAniwaveServer
    override var name = "9Anime"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedSyncNames = setOf(
        SyncIdName.Anilist,
        SyncIdName.MyAnimeList
    )
    override val supportedTypes = setOf(TvType.Anime)
    override val hasQuickSearch = true

    //private val vrfInterceptor by lazy { JsVrfInterceptor(mainUrl) }
    companion object {
        fun encode(input: String): String =
            java.net.URLEncoder.encode(input, "utf-8").replace("+", "%2B")

        private fun decode(input: String): String = java.net.URLDecoder.decode(input, "utf-8")
//        private const val consuNineAnimeApi = "https://api.consumet.org/anime/9anime"

    }

    override val mainPage = mainPageOf(
        "$mainUrl/ajax/home/widget/trending?page=" to "Trending",
        "$mainUrl/ajax/home/widget/updated-all?page=" to "All",
        "$mainUrl/ajax/home/widget/updated-sub?page=" to "Recently Updated (SUB)",
        "$mainUrl/ajax/home/widget/updated-dub?page=" to "Recently Updated (DUB)",
        "$mainUrl/ajax/home/widget/updated-china?page=" to "Recently Updated (Chinese)",
        "$mainUrl/ajax/home/widget/random?page=" to "Random",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = request.data + page
        //vrfInterceptor.wake()
        val home = Jsoup.parse(
            app.get(
                url,

                ).parsed<Response>().html!!
        ).select("div.item").mapNotNull { element ->
            val title = element.selectFirst(".info > .name") ?: return@mapNotNull null
            val link = title.attr("href").replace(Regex("/ep.*\$"), "")
            val poster = element.selectFirst(".poster > a > img")?.attr("src")
            val meta = element.selectFirst(".poster > a > .meta > .inner > .left")
            val subbedEpisodes = meta?.selectFirst(".sub")?.text()?.toIntOrNull()
            val dubbedEpisodes = meta?.selectFirst(".dub")?.text()?.toIntOrNull()

            newAnimeSearchResponse(title.text() ?: return@mapNotNull null, link) {
                this.posterUrl = poster
                addDubStatus(
                    dubbedEpisodes != null,
                    subbedEpisodes != null,
                    dubbedEpisodes,
                    subbedEpisodes
                )
            }
        }

        return newHomePageResponse(request.name, home, true)
    }

    data class Response(
        @JsonProperty("result") val html: String?,
        @JsonProperty("llaa") var llaa: String? = null,
        @JsonProperty("epurl") var epurl: String? = null
    )


    override suspend fun quickSearch(query: String): List<SearchResponse> {
        delay(1000)
        return search(query)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url =
            "$mainUrl/filter?keyword=${query}"
        return app.get(
            url,
        ).document.select("#list-items div.inner:has(div.poster)").mapNotNull {
            val a = it.select("a.d-title")
            val link = fixUrl(a.attr("href") ?: return@mapNotNull null)
            val img = it.select("img")
            val title = a.text()
            val subbedEpisodes = it?.selectFirst(".sub")?.text()?.toIntOrNull()
            val dubbedEpisodes = it?.selectFirst(".dub")?.text()?.toIntOrNull()
            newAnimeSearchResponse(title, link) {
                posterUrl = img.attr("src")
                addDubStatus(
                    dubbedEpisodes != null,
                    subbedEpisodes != null,
                    dubbedEpisodes,
                    subbedEpisodes
                )

            }
        }
    }


    /*   private fun Int.toBoolean() = this == 1
     data class EpsInfo (
         @JsonProperty("llaa"     ) var llaa     : String?  = null,
         @JsonProperty("epurl"    ) var epurl    : String?  = null,
         @JsonProperty("needDUB" ) var needDub : Boolean? = null,
         )
     private fun Element.toGetEpisode(url: String, needDub: Boolean):Episode{
           //val ids = this.attr("data-ids").split(",", limit = 2)
           val epNum = this.attr("data-num")
               .toIntOrNull() // might fuck up on 7.5 ect might use data-slug instead
           val epTitle = this.selectFirst("span.d-title")?.text()
           val epurl = "$url/ep-$epNum"
           val data = "{\"llaa\":\"null\",\"epurl\":\"$epurl\",\"needDUB\":$needDub}"
          return Episode(
               data,
               epTitle,
               episode = epNum
           )
       } */

    override suspend fun load(url: String): LoadResponse {
        val validUrl =
            url.replace("https://9anime.to", mainUrl).replace("https://aniwave.to", mainUrl)
        val doc = app.get(
            validUrl,
        ).document

        val meta = doc.selectFirst("#w-info") ?: throw ErrorLoadingException("Could not find info")
        val ratingElement = meta.selectFirst(".brating > #w-rating")
        val id = ratingElement?.attr("data-id") ?: throw ErrorLoadingException("Could not find id")
        val binfo =
            meta.selectFirst(".binfo") ?: throw ErrorLoadingException("Could not find binfo")
        val info = binfo.selectFirst(".info") ?: throw ErrorLoadingException("Could not find info")
        val poster = binfo.selectFirst(".poster > span > img")?.attr("src")
        val backimginfo = doc.selectFirst("#player")?.attr("style")
        val backimgRegx = Regex("(http|https).*jpg")
        val backposter = backimgRegx.find(backimginfo.toString())?.value ?: poster
        val title = (info.selectFirst(".title") ?: info.selectFirst(".d-title"))?.text()
            ?: throw ErrorLoadingException("Could not find title")
        //val vvhelp = consumetVrf(id)
        //val vrf = encode(vvhelp.url)
        val vrf = AniwaveUtils.vrfEncrypt(id)
        val episodeListUrl = "$mainUrl/ajax/episode/list/$id?$vrf"
        val body =
            app.get(episodeListUrl).parsedSafe<Response>()?.html
                ?: throw ErrorLoadingException("Could not parse json with Vrf=$vrf id=$id url=\n$episodeListUrl")

        val subEpisodes = ArrayList<Episode>()
        val dubEpisodes = ArrayList<Episode>()
        val softsubeps = ArrayList<Episode>()
        val uncensored = ArrayList<Episode>()
        val genres =
            doc.select("div.meta:nth-child(1) > div:contains(Genre:) a").mapNotNull { it.text() }
        val recss =
            doc.select("div#watch-second .w-side-section div.body a.item").mapNotNull { rec ->
                val href = rec.attr("href")
                val rectitle = rec.selectFirst(".name")?.text() ?: ""
                val recimg = rec.selectFirst("img")?.attr("src")
                newAnimeSearchResponse(rectitle, fixUrl(href)) {
                    this.posterUrl = recimg
                }
            }
        val status =
            when (doc.selectFirst("div.meta:nth-child(1) > div:contains(Status:) span")?.text()) {
                "Releasing" -> ShowStatus.Ongoing
                "Completed" -> ShowStatus.Completed
                else -> null
            }

        val typetwo =
            when (doc.selectFirst("div.meta:nth-child(1) > div:contains(Type:) span")?.text()) {
                "OVA" -> TvType.OVA
                "SPECIAL" -> TvType.OVA
                //"MOVIE" -> TvType.AnimeMovie
                else -> TvType.Anime
            }
        val duration = doc.selectFirst(".bmeta > div > div:contains(Duration:) > span")?.text()

        Jsoup.parse(body).body().select(".episodes > ul > li > a").apmap { element ->
            val ids = element.attr("data-ids").split(",", limit = 3)
            val dataDub = element.attr("data-dub").toIntOrNull()
            val epNum = element.attr("data-num")
                .toIntOrNull() // might fuck up on 7.5 ect might use data-slug instead
            val epTitle = element.selectFirst("span.d-title")?.text()
            val isUncen = element.attr("data-slug").contains("uncen")
            //val filler = element.hasClass("filler")

            //season -1 HARDSUBBED
            //season -2 Dubbed
            //Season -3 SofSubbed
            //Season -4 Uncensored
            //SUB, SOFT SUB and DUB adb logcat -s "TAGNAME"

            if (ids.size > 0) {
                if(isUncen) {
                    ids.getOrNull(0)?.let { uncen ->
                        val epdd = "{\"ID\":\"$uncen\",\"type\":\"sub\"}"
                        uncensored.add(
                            newEpisode(epdd) {
                                this.episode = epNum
                                this.name = epTitle
                                this.season = -4
                            }
                        )
                    }
                } else {
                    if (ids.size == 1 && dataDub == 1) {
                        ids.getOrNull(0)?.let { dub ->
                            val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
                            dubEpisodes.add(
                                newEpisode(epdd) {
                                    this.episode = epNum
                                    this.name = epTitle
                                    this.season = -2
                                }
                            )
                        }
                    } else {
                        ids.getOrNull(0)?.let { sub ->
                            val epdd = "{\"ID\":\"$sub\",\"type\":\"sub\"}"
                            subEpisodes.add(
                                newEpisode(epdd) {
                                    this.episode = epNum
                                    this.name = epTitle
                                    this.season = -1
                                }
                            )
                        }
                    }
                }
                if (ids.size > 1) {
                    if (dataDub == 0 || ids.size > 2) {
                        ids.getOrNull(1)?.let { softsub ->
                            val epdd = "{\"ID\":\"$softsub\",\"type\":\"softsub\"}"
                            softsubeps.add(
                                newEpisode(epdd) {
                                    this.episode = epNum
                                    this.name = epTitle
                                    this.season = -3
                                }
                            )
                        }
                    } else {
                        ids.getOrNull(1)?.let { dub ->
                            val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
                            dubEpisodes.add(
                                newEpisode(epdd) {
                                    this.episode = epNum
                                    this.name = epTitle
                                    this.season = -2
                                }
                            )
                        }
                    }

                    if (ids.size > 2) {
                        ids.getOrNull(2)?.let { dub ->
                            val epdd = "{\"ID\":\"$dub\",\"type\":\"dub\"}"
                            dubEpisodes.add(
                                newEpisode(epdd) {
                                    this.episode = epNum
                                    this.name = epTitle
                                    this.season = -2
                                }
                            )
                        }
                    }
                }
            }
        }

        //season -1 HARDSUBBED
        //season -2 Dubbed
        //Season -3 SofSubbed

        println("SUBstat ${DubStatus.Subbed.name}")
        println("SUBstat ${DubStatus.Subbed.toString()}")

        val names = listOf(
            Pair("Sub", -1),
            Pair("Dub", -2),
            Pair("S-Sub", -3),
            Pair("Uncensored", -4),
        )
        
        //Reading info from web page to fetch anilistData
        val titleRomaji = (info.selectFirst(".title") ?: info.selectFirst(".d-title"))?.attr("data-jp") ?: ""
        val premieredDetails = info.select(".bmeta > .meta > div").find {
            it.text().contains("Premiered: ", true)
        }?.selectFirst("span > a")?.text()?.split(" ")
        val season = premieredDetails?.get(0).toString()
        val year = premieredDetails?.get(1)?.toInt() ?: 0

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            addEpisodes(DubStatus.Subbed, dubEpisodes)
            addEpisodes(DubStatus.Subbed, subEpisodes)
            addEpisodes(DubStatus.Subbed, softsubeps)
            addEpisodes(DubStatus.Subbed, uncensored)
            this.seasonNames = names.map { (name, int) -> SeasonData(int, name) }
            plot = info.selectFirst(".synopsis > .shorting > .content")?.text()
            this.posterUrl = poster
            rating = ratingElement.attr("data-score").toFloat().times(1000f).toInt()
            this.backgroundPosterUrl = backposter
            this.tags = genres
            this.recommendations = recss
            this.showStatus = status
            if (AniwaveProviderPlugin.aniwaveSimklSync)
                addAniListId(aniAPICall(AniwaveUtils.aniQuery( titleRomaji, year, season))?.id)
            else
                this.type = typetwo
            addDuration(duration)
        }
    }

    data class Result(
        @JsonProperty("url")
        val url: String? = null
    )

    data class Links(
        @JsonProperty("result")
        val result: Result? = null
    )

    /*private suspend fun getEpisodeLinks(id: String): Links? {
        return app.get("$mainUrl/ajax/server/$id?vrf=encodeVrf(id, cipherKey)}").parsedSafe()
    }*/

    /*   private suspend fun getStream(
           streamLink: String,
           name: String,
           referer: String,
           callback: (ExtractorLink) -> Unit
       )  {
           return generateM3u8(
               name,
               streamLink,
               referer
           ).map {
               callback(
                   ExtractorLink(
                       ""
                   )
               )
           }
       } */


    /*  private suspend fun getM3U8(epurl: String, lang: String, callback: (ExtractorLink) -> Unit):Boolean{
          val isdub = lang == "dub"
          val vidstream = app.get(epurl, interceptor = JsInterceptor("41", lang), timeout = 45)
          val mcloud = app.get(epurl, interceptor = JsInterceptor("28", lang), timeout = 45)
          val vidurl = vidstream.url
          val murl = mcloud.url
          val ll = listOf(vidurl, murl)
          ll.forEach {link ->
              val vv = link.contains("mcloud")
              val name1 = if (vv) "Mcloud" else "Vidstream"
              val ref = if (vv) "https://mcloud.to/" else ""
              val name2 = if (isdub) "$name1 Dubbed" else "$name1 Subbed"
              getStream(link, name2, ref ,callback)
          }
          return true
      } */

    data class NineConsumet(
        @JsonProperty("headers") var headers: ServerHeaders? = ServerHeaders(),
        @JsonProperty("sources") var sources: ArrayList<NineConsuSources>? = arrayListOf(),
        @JsonProperty("embedURL") var embedURL: String? = null,

        )

    data class NineConsuSources(
        @JsonProperty("url") var url: String? = null,
        @JsonProperty("isM3U8") var isM3U8: Boolean? = null
    )

    data class ServerHeaders(

        @JsonProperty("Referer") var referer: String? = null,
        @JsonProperty("User-Agent") var userAgent: String? = null

    )

    data class SubDubInfo(
        @JsonProperty("ID") val ID: String,
        @JsonProperty("type") val type: String
    )

    private fun serverName(serverID: String?): String? {
        val sss =
            when (serverID) {
                "41" -> "vidplay"
                "44" -> "filemoon"
                "40" -> "streamtape"
                "35" -> "mp4upload"
                "28" -> "MyCloud"
                else -> null
            }
        return sss
    }


    data class ConsumetVrfHelper(
        @JsonProperty("url") var url: String,
        @JsonProperty("vrfQuery") var vrfQuery: String
    )

    private suspend fun consumetVrf(input: String): ConsumetVrfHelper {
        return app.get("https://9anime.eltik.net/vrf?query=$input&apikey=lagrapps")
            .parsed<ConsumetVrfHelper>()
    }

    private suspend fun decUrlConsu(serverID: String): String {
        val sa = consumetVrf(serverID)
        val encID = sa.url
        val videncrr = app.get("$mainUrl/ajax/server/$serverID?${sa.vrfQuery}=${encode(encID)}")
            .parsed<Links>()
        val encUrl = videncrr.result?.url
        val ses = app.get("https://9anime.eltik.net/decrypt?query=$encUrl&apikey=lagrapps").text
        return ses.substringAfter("url\":\"").substringBefore("\"")
    }

    data class AniwaveMediaInfo(

        @JsonProperty("result") val result: AniwaveResult? = AniwaveResult()

    )


    data class AniwaveResult(

        @JsonProperty("sources") var sources: ArrayList<AniwaveTracks> = arrayListOf(),
        @JsonProperty("tracks") var tracks: ArrayList<AniwaveTracks> = arrayListOf()

    )

    data class AniwaveTracks(
        @JsonProperty("file") var file: String? = null,
        @JsonProperty("label") var label: String? = null,
    )

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val parseData = AppUtils.parseJson<SubDubInfo>(data)
        //val sa = consumetVrf(parseData.ID)
        val datavrf = AniwaveUtils.vrfEncrypt(parseData.ID)
        val one = app.get("$mainUrl/ajax/server/list/${parseData.ID}?$datavrf").parsed<Response>()
        val two = Jsoup.parse(one.html ?: return false)
        val aas = two.select("div.servers .type[data-type=${parseData.type}] li").mapNotNull {
            val datalinkId = it.attr("data-link-id")
            val serverID = it.attr("data-sv-id").toString()
            val newSname = serverName(serverID)
            Pair(newSname, datalinkId)
        }
        aas.amap { (sName, sId) ->
//            val nName = sName ?: "mycloud"
//            val vids = nName == "vidplay"
//            val mclo = nName == "mycloud"
//            if (vids || mclo) {
//                val sae = consumetVrf(sId)
//                val encID = sae.url
            val vrf = AniwaveUtils.vrfEncrypt(sId)
            val videncrr = app.get("$mainUrl/ajax/server/$sId?$vrf").parsed<Links>()
            val encUrl = videncrr.result?.url ?: return@amap

//                    val asss = decUrlConsu(sId)
            val asss = AniwaveUtils.vrfDecrypt(encUrl)

            if(sName.equals("filemoon")) {
                val res = app.get(asss)
                if(res.code == 200) {
                    val packedJS = res.document.selectFirst("script:containsData(function(p,a,c,k,e,d))")?.data().toString()
                    JsUnpacker(packedJS).unpack().let { unPacked ->
                        Regex("sources:\\[\\{file:\"(.*?)\"").find(unPacked ?: "")?.groupValues?.get(1)?.let { link ->
                            callback.invoke(ExtractorLink("Filemoon", "Filemoon", link, "", Qualities.Unknown.value, link.contains(".m3u8")))
                        }
                    }
                }
            } else loadExtractor(asss, subtitleCallback, callback)

//                val regex = Regex("(.+?/)e(?:mbed)?/([a-zA-Z0-9]+)")
//                val group = regex.find(asss)!!.groupValues
//                val comps = asss.split("/");
//                val vizId = comps[comps.size - 1];
//                val action = if (vids) "rawVizcloud" else "rawMcloud"
//                val futoken = app.get("https://vidstream.pro/futoken").text
//                val encodedFutoken = URLEncoder.encode(futoken, "UTF-8")
//                val map = mapOf("query" to vizId, "futoken" to futoken)
//                val jsonBody = JSONObject(map).toString()
//                val mediaType = "application/json; charset=utf-8".toMediaType()
//                val ssaeUrl = app.post(
//                    "https://9anime.eltik.net/$action?apikey=lagrapps",
//                    mapOf("Content-Type" to "application/x-www-form-urlencoded"),
//                    requestBody = RequestBody.Companion.create(mediaType, jsonBody)
//                ).text.substringAfter("rawURL\"").substringAfter("\"").substringBefore("\"");
//
//                val ref = if (vids) "https://vidstream.pro/" else "https://mcloud.to/"
//
//                //val ssae = app.get(ssaeUrl, headers = mapOf("Referer" to ref)).text
//
//                val resultJson = app.get(ssaeUrl, headers = mapOf("Referer" to ref))
//                    .parsedSafe<AniwaveMediaInfo>()
//                val name = if (vids) "Vidplay" else "MyCloud"
//                resultJson?.result?.sources?.amap {
//                    val source = it.file ?: ""
//                    generateM3u8(
//                        name,
//                        source,
//                        ref
//                    ).forEach(callback)
//                }
//                resultJson?.result?.tracks?.amap {
//                    val subtitle = it.file ?: ""
//                    val lang = it.label ?: ""
//                    subtitleCallback.invoke(
//                        SubtitleFile(lang, subtitle)
//                    )
//                }
//            }
//            if (!sName.isNullOrEmpty() && !vids || !mclo) {
//                val bbbs = decUrlConsu(sId)
//                loadExtractor(bbbs, subtitleCallback, callback)
//            }
        }
        return true
    }

    override suspend fun getLoadUrl(name: SyncIdName, id: String): String? {
        val syncId = id.split("/").last()

        //formatting the JSON response to search on aniwave site
        val anilistData = aniAPICall(AniwaveUtils.aniQuery(name, syncId.toInt()))
        val title = anilistData?.title?.romaji ?: anilistData?.title?.english
        val year = anilistData?.year
        val season = anilistData?.season
        val searchUrl = "$mainUrl/filter?keyword=${title}&year%5B%5D=${year}&season%5B%5D=unknown&season%5B%5D=${season?.lowercase()}&sort=recently_updated"

        //searching the anime on aniwave site using advance filter and capturing the url from search result
        val document = app.get(searchUrl).document
        val syncUrl = document.select("#list-items div.info div.b1 > a").find {
            it.attr("data-jp").equals(title, true)
        }?.attr("href")
        return fixUrl(syncUrl ?: return null)

    }

    private suspend fun aniAPICall(query: String): Media? {
        //Fetching data using POST method
        val url = "https://graphql.anilist.co"
        val res = app.post(url,
            headers = mapOf(
                "Accept"  to "application/json",
                "Content-Type" to "application/json",
            ),
            data = mapOf(
                "query" to query,
            )
        ).parsedSafe<SyncInfo>()

        return res?.data?.media
    }
    
    //JSON formatter for data fetched from anilistApi
    data class SyncTitle(
        @JsonProperty("romaji") val romaji: String? = null,
        @JsonProperty("english") val english: String? = null,
    )

    data class Media(
        @JsonProperty("title") val title: SyncTitle? = null,
        @JsonProperty("id") val id: Int? = null,
        @JsonProperty("idMal") val idMal: Int? = null,
        @JsonProperty("season") val season: String? = null,
        @JsonProperty("seasonYear") val year: Int? = null,
    )
    
    data class Data(
        @JsonProperty("Media") val media: Media? = null,
    )
    
    data class SyncInfo(
        @JsonProperty("data") val data: Data? = null,
    )
}
