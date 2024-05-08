package com.QueenMedusa

import com.lustyflix.streamverse.*
import com.lustyflix.streamverse.LoadResponse.Companion.addActors
import com.lustyflix.streamverse.LoadResponse.Companion.addTrailer
import com.lustyflix.streamverse.mvvm.safeApiCall
import com.lustyflix.streamverse.utils.ExtractorLink
import com.lustyflix.streamverse.utils.loadExtractor
import org.jsoup.nodes.Element
import java.net.URI

open class EroticMovies : MainAPI() {
    override var mainUrl = "https://www.film1k.com"
    private var directUrl = ""
    override var name = "Erotic Movies"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie)


    override val mainPage = mainPageOf(
        "category/action" to "Action",
        "category/adventure" to "Adventure",
        "category/comedy" to "Comedy",
        "category/crime" to "Crime",
        "category/documentary" to "Documentary",
        "category/drama" to "Drama",
        "category/fantasy" to "Fantasy",
        "category/horror" to "Horror",
        "category/mystery" to "Mystery",
        "category/romance" to "Romance",
        "category/sci-fi" to "Sci-Fi",
        "category/thriller" to "Thriller",
        "" to "Popular Movies",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("article.loop-post.vdeo.snow-b.sw03.pd08.por.ovh").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("header.mt08 h2")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a.lka")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("div.thumb.por img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("article.loop-post.vdeo.snow-b.sw03.pd08.por.ovh").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val request = app.get(url)
        directUrl = getBaseUrl(request.url)
        val document = request.document
        val title = document.selectFirst("h1.ttl.f24.h123-c.py08.mr16")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("img.alignleft")?.attr("src"))
        val tags = document.select("div.tagcloud.mt08 a").map { it.text() }
        //val year = document.select("div.entry.pd16.c-pd24 a").text().trim().toIntOrNull()
        //val description = document.selectFirst("div.Description p")?.text()?.trim()

        //val rating = document.select("div.mvici-right > div.imdb_r span").text().toRatingInt()
        //val actors = document.select("ul#CastUl li a").map { it.text() }
        val recommendations = document.select("article.loop-post.vdeo.snow-b.sw03.pd08.por.ovh").mapNotNull {
            it.toSearchResult()
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                //this.year = year
                //this.plot = description
                this.tags = tags
                //this.rating = rating
                //addActors(actors)
                this.recommendations = recommendations
                
        }
        
        
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        doc.select("iframe").forEach {
            loadExtractor(fixUrl(it.attr("src").replace("films5k.com", "streamwish.to")), data, subtitleCallback, callback)
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
