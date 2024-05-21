package com.QueenMedusa

import com.lustyflix.streamverse.*
import com.lustyflix.streamverse.LoadResponse.Companion.addActors
import com.lustyflix.streamverse.LoadResponse.Companion.addTrailer
import com.lustyflix.streamverse.mvvm.safeApiCall
import com.lustyflix.streamverse.utils.ExtractorLink
import com.lustyflix.streamverse.utils.loadExtractor
import org.jsoup.nodes.Element
import java.net.URI

open class M4uHD : MainAPI() {
    override var mainUrl = "https://ww1.m4uhd.tv"
    private var directUrl = ""
    override var name = "M4uHD"
    override val hasMainPage = true
    override var lang = "bn"
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )

    override val mainPage = mainPageOf(
        "top-movies" to "Top Movies",
        "new-movies" to "New Movies",
        "top-tv-series" to "TOP Series",
        "new-tv-series" to "NEW Episodes",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}?page=$page").document
        val home = document.select("div.item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href").toString())
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/search/{$query}.html").document

        return document.select("div.item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val request = app.get(url)
        directUrl = getBaseUrl(request.url)
        val document = request.document
        val title = document.selectFirst("ol.breadcrumb li:nth-child(3)")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("img.mvinfo")?.attr("src"))
        val tags = document.select("ol li:nth-child(3) h4.h3-detail span").map { it.text() }
        val year = document.select("div.mvici-right p:nth-child(3) a").text().trim()
            .toIntOrNull()
        val tvType = if (document.selectFirst("button.episode")?.size!! > 1")) == true
        ) TvType.TvSeries else TvType.Movie
        val description = document.selectFirst("pre")?.text()?.trim()
        val trailer = fixUrlNull(document.select("iframe#iframe-trailer").attr("src"))
        val rating = document.select("ol li:nth-child(1) h4.h3-detail span").text().toRatingInt()
        val actors = document.select("ol li:nth-child(4) h5.h3-detail span").map { it.text() }
        val recommendations = document.select("div.item").mapNotNull {
            it.toSearchResult()
        }

        return if (tvType == TvType.TvSeries) {
            val episodes = if (document.selectFirst("div.les-title strong")?.text().toString()
                    .contains(Regex("(?i)EP\\s?[0-9]+|Episode\\s?[0-9]+"))
            ) {
                document.select("ul.idTabs li").map {
                    val id = it.select("a").attr("href")
                    Episode(
                        data = fixUrl(document.select("div$id iframe").attr("src")),
                        name = it.select("strong").text(),
                    )
                }
            } else {
                document.select("div.les-content a").map {
                    Episode(
                        data = it.attr("href"),
                        name = it.text().trim(),
                    )
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                addActors(actors)
                this.recommendations = recommendations
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
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

        if (data.contains(directUrl.getHost(), true)) {
            val doc = app.get(data).document
            doc.select("div.movieplay iframe").map { fixUrl(it.attr("src")) }
                .apmap { source ->
                    safeApiCall {
                        when {
                            source.startsWith("https://membed.net") -> app.get(
                                source,
                                referer = "$directUrl/"
                            ).document.select("ul.list-server-items li")
                                .apmap {
                                    loadExtractor(
                                        it.attr("data-video").substringBefore("=https://msubload"),
                                        "$directUrl/",
                                        subtitleCallback,
                                        callback
                                    )
                                }
                            else -> loadExtractor(source, "$directUrl/", subtitleCallback, callback)
                        }
                    }
                }
        } else {
            loadExtractor(data, "$directUrl/", subtitleCallback, callback)
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
