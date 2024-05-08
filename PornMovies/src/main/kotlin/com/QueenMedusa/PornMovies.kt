package com.QueenMedusa

import com.lustyflix.streamverse.*
import com.lustyflix.streamverse.LoadResponse.Companion.addActors
import com.lustyflix.streamverse.LoadResponse.Companion.addTrailer
import com.lustyflix.streamverse.mvvm.safeApiCall
import com.lustyflix.streamverse.utils.ExtractorLink
import com.lustyflix.streamverse.utils.loadExtractor
import org.jsoup.nodes.Element
import java.net.URI

open class PornMovies : MainAPI() {
    override var mainUrl = "https://bananamovies.org"
    private var directUrl = ""
    override var name = "Porn Movies"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(TvType.Movie)


    override val mainPage = mainPageOf(
        "director/evil-angel" to "Evil Angel",
        "director/new-sensations" to "New Sensations",
        "director/brazzers" to "Brazzers",
        "director/reality-kings" to "Reality Kings",
        "director/jules-jordan-video" to "Jules Jordan Video",
        "director/lethal-hardcore" to "Lethal Hardcore",
        "director/team-skeet" to "Team Skeet",
        "director/mofos" to "MOFOS",
        "director/digital-sin" to "Digital Sin",
        "director/elegant-angel" to "Elegant Angel",
        "director/wicked-pictures" to "Wicked Pictures",
        "director/digital-playground" to "Digital Playground",
        "director/devils-film" to "Devils Film",
        "director/bang-bros-productions" to "Bang Bros",
        "director/3rd-degree" to "3rd Degree",
        "director/pornfidelity" to "Pornfidelity",
        "director/letsdoeit" to "LETSDOEIT",
        "director/private" to "Private",
        "director/marc-dorcel" to "Marc Dorcel",
        "director/zero-tolerance-ent" to "Zero Tolerance",
        "director/21-sextury-video" to "21 Sextury",
        "director/naughty-america" to "Naughty America",
        "director/bluebird-films" to "Bluebird Films",
        "director/adam-eve" to "Adam Eve",
        "director/bang" to "Bang",
        "director/hardx" to "HardX",
        "movies" to "Popular Porn Movies",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("article.TPost.B").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("div.Title")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("data-lazy-src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?s=$query").document

        return document.select("article.TPost.B").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val request = app.get(url)
        directUrl = getBaseUrl(request.url)
        val document = request.document
        val title = document.selectFirst("h1.Title")?.text()?.replace(" Porn Movie Online Free", "")?.replace("Watch ", "")?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.Image figure img")?.attr("data-lazy-src"))
        val tags = document.select("p.Director span:nth-child(1) a").map { it.text() }
        val year = document.select("span.Views a").text().trim()
            .toIntOrNull()
        val description = document.selectFirst("div.Description p")?.text()?.trim()

        //val rating = document.select("div.mvici-right > div.imdb_r span").text().toRatingInt()
        val actors = document.select("ul#CastUl li a").map { it.text() }
        val recommendations = document.select("div.TPost.B").mapNotNull {
            it.toSearchResult()
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                //this.rating = rating
                addActors(actors)
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
