package com.xxyxxdmc.musicplayer

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import java.io.File

@Service
@State(name = "MusicPlayerSettings", storages = [Storage("music-player.xml")])
class MusicPlayerSettings : PersistentStateComponent<MusicPlayerSettings> {
    var musicFolder: String? = null
    var currentMusic: File? = null
    var playCase: Int = 0

    override fun getState(): MusicPlayerSettings {
        return this
    }

    override fun loadState(@NotNull state: MusicPlayerSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
