package com.xxyxxdmc.musicplayer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.File

@State(name = "MusicPlayerSettings", storages = [Storage("MusicPlayerSettings.xml")])
class MusicPlayerSettings : PersistentStateComponent<MusicPlayerSettings> {
    var musicFolder: String? = null
    var currentMusic: File? = null
    var playCase: Int = 0

    @Nullable
    override fun getState(): MusicPlayerSettings {
        return this
    }

    override fun loadState(@NotNull state: MusicPlayerSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: MusicPlayerSettings
            get() = ApplicationManager.getApplication().getService(MusicPlayerSettings::class.java)
    }

}
