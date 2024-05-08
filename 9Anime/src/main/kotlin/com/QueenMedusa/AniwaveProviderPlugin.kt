package com.QueenMedusa

import com.lustyflix.streamverse.plugins.StreamversePlugin
import com.lustyflix.streamverse.plugins.Plugin
import android.content.Context
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.QueenMedusa.BottomFragment
import com.lustyflix.streamverse.AcraApplication.Companion.getActivity
import com.lustyflix.streamverse.AcraApplication.Companion.getKey
import com.lustyflix.streamverse.AcraApplication.Companion.setKey
import com.lustyflix.streamverse.MainActivity.Companion.afterPluginsLoadedEvent
import com.lustyflix.streamverse.plugins.PluginManager

enum class ServerList(val link: String) {
    TO("https://aniwave.to"),
    LI("https://aniwave.li"),
    VC("https://aniwave.vc")
}

@StreamversePlugin
class AniwaveProviderPlugin : Plugin() {
    override fun load(context: Context) {
        // All providers should be added in this manner. Please don't edit the providers list directly.
        registerMainAPI(AniwaveProvider())

        this.openSettings = openSettings@{
            val manager = (context.getActivity() as? AppCompatActivity)?.supportFragmentManager
                ?: return@openSettings
               
            BottomFragment(this).show(manager, "")
        }
    }

    fun reload(context: Context?) {
        val pluginData = PluginManager.getPluginsOnline().find { it.internalName.contains("Aniwave") }
        if(pluginData == null) {
            PluginManager.hotReloadAllLocalPlugins(context as AppCompatActivity)
        } else {
            PluginManager.unloadPlugin(pluginData.filePath)
            PluginManager.loadAllOnlinePlugins(context!!)
            afterPluginsLoadedEvent.invoke(true)
        }
    }

    companion object {
        /**
         * Used to make Runnables work properly on Android 21
         * Otherwise you get:
         * ERROR:D8: Invoke-customs are only supported starting with Android O (--min-api 26)
         **/
        inline fun Handler.postFunction(crossinline function: () -> Unit) {
            this.post(object : Runnable {
                override fun run() {
                    function()
                }
            })
        }
        
        var currentAniwaveServer: String
            get() = getKey("ANIWAVE_CURRENT_SERVER") ?: ServerList.TO.link
            set(value) {
                setKey("ANIWAVE_CURRENT_SERVER", value)
            }
        
        var aniwaveSimklSync: Boolean
            get() = getKey("ANIWAVE_SIMKL_SYNC") ?: false
            set(value) {
                setKey("ANIWAVE_SIMKL_SYNC", value)
            }
    }
}