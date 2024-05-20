
package com.QueenMedusa

import com.lustyflix.streamverse.plugins.StreamversePlugin
import com.lustyflix.streamverse.plugins.Plugin
import android.content.Context
import com.QueenMedusa.Alions
import com.QueenMedusa.Animefever
import com.QueenMedusa.Comedyshow
import com.QueenMedusa.Embedrise
import com.QueenMedusa.Embedwish
import com.QueenMedusa.FilelionsTo
import com.QueenMedusa.FilemoonNl
import com.QueenMedusa.Flaswish
import com.QueenMedusa.Gdmirrorbot
import com.QueenMedusa.M4ufree
import com.QueenMedusa.Multimovies
import com.QueenMedusa.MultimoviesSB
import com.QueenMedusa.Mwish
import com.QueenMedusa.Netembed
import com.QueenMedusa.Playm4u
import com.QueenMedusa.Ridoo
import com.QueenMedusa.Streamverse
import com.QueenMedusa.Streamruby
import com.QueenMedusa.Streamvid
import com.QueenMedusa.Streamwish
import com.QueenMedusa.TravelR
import com.QueenMedusa.Uploadever
import com.QueenMedusa.UqloadsXyz
import com.QueenMedusa.VCloud
import com.QueenMedusa.Yipsu

@StreamversePlugin
class StreamversePlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(Streamverse())
        registerExtractorAPI(Animefever())
        registerExtractorAPI(Multimovies())
        registerExtractorAPI(MultimoviesSB())
        registerExtractorAPI(Yipsu())
        registerExtractorAPI(Mwish())
        registerExtractorAPI(TravelR())
        registerExtractorAPI(Playm4u())
        registerExtractorAPI(VCloud())

        registerExtractorAPI(M4ufree())
        registerExtractorAPI(Streamruby())
        registerExtractorAPI(Streamwish())
        registerExtractorAPI(FilelionsTo())
        registerExtractorAPI(Embedwish())
        registerExtractorAPI(UqloadsXyz())
        registerExtractorAPI(Uploadever())
        registerExtractorAPI(Netembed())
        registerExtractorAPI(Flaswish())
        registerExtractorAPI(Comedyshow())
        registerExtractorAPI(Ridoo())
        registerExtractorAPI(Streamvid())
        registerExtractorAPI(Embedrise())
        registerExtractorAPI(Gdmirrorbot())
        registerExtractorAPI(FilemoonNl())
        registerExtractorAPI(Alions())
    }
}