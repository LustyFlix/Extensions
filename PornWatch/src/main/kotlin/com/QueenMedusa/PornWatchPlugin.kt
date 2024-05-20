
package com.QueenMedusa

import com.lustyflix.streamverse.plugins.StreamversePlugin
import com.lustyflix.streamverse.plugins.Plugin
import android.content.Context

@StreamversePlugin
class PornWatchPlugin: Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(PornWatch())
    }
}