package com.QueenMedusa

import com.lustyflix.streamverse.*
import com.lustyflix.streamverse.LoadResponse.Companion.addActors
import com.lustyflix.streamverse.LoadResponse.Companion.addTrailer
import com.lustyflix.streamverse.mvvm.safeApiCall
import com.lustyflix.streamverse.utils.ExtractorLink
import com.lustyflix.streamverse.utils.loadExtractor
import org.jsoup.nodes.Element
import java.net.URI

open class PornWatch : MainAPI() {
    override var mainUrl = "https://pornwatch.ws/"
    private var directUrl = ""
    override var name = "PornWatch"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.NSFW,
    )
    override val vpnStatus = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "genre/18-teens" to "18+ Teens",
        "genre/amateurs" to "Amateurs",
        "genre/all-girl" to "All Girl",
        "genre/bdsm" to "BDSM",
        "genre/big-boobs" to "Big Boobs",
        "genre/big-cocks" to "Big Cocks",
        "genre/creampie" to "Creampie",
        "genre/cumshots" to "Cumshots",
        "genre/deep-throat" to "Deep Throat",
        "genre/double-penetration" to "Double Penetration",
        "genre/facials" to "Facials",
        "genre/fingering" to "Fingering",
        "genre/fetish" to "Fetish",
        "genre/gangbang" to "Gangbang",
        "genre/gonzo" to "Gonzo",
        "genre/group-sex" to "Group Sex",
        "genre/hardcore" to "Hardcore",
        "genre/lesbian" to "Lesbian",
        "genre/massage" to "Massage",
        "genre/orgy" to "Orgy",
        "genre/squirting" to "Squirting",
        "genre/swallowing" to "Swallowing",
        "genre/transsexual" to "Transsexual",
        "genre/virgin" to "Virgin",
        "series" to "All TV Series",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("div.ml-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.qtip-title")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("div.ml-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val request = app.get(url)
        directUrl = getBaseUrl(request.url)
        val document = request.document
        val title = document.selectFirst("div.mvic-desc h3")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.thumb.mvic-thumb img")?.attr("src"))
        val tags = document.select("div.mvici-left p:nth-child(1) a").map { it.text() }
        val year = document.select("div.mvici-right p:nth-child(3) a").text().trim()
            .toIntOrNull()
        val tvType = TvType.NSFW
        val description = document.selectFirst("p.f-desc")?.text()?.trim()
        val trailer = fixUrlNull(document.select("iframe#iframe-trailer").attr("src"))
        val rating = document.select("div.mvici-right > div.imdb_r span").text().toRatingInt()
        val actors = document.select("div.mvici-left p:nth-child(3) a").map { it.text() }
        val recommendations = document.select("div.ml-item").mapNotNull {
            it.toSearchResult()
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                addActors(actors)
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        doc.select("div.Rtable1-cell a").forEach {
            loadExtractor(fixUrl(it.attr("href")), data, subtitleCallback, callback)
        }
        
        return true
    }

    private fun String.getHost(): String {
        return fixTitle(URI(this).host.substringBeforeLast(".").substringAfterLast("."))
    }

    private fun getBaseUrl(url: String): String {
        return URI(url).let {
            "${it.scheme}://${it.host}"
        }
    }

}
