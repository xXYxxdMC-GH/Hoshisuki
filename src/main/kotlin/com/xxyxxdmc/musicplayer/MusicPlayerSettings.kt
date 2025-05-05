package com.xxyxxdmc.musicplayer

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

@Service
@State(name = "MusicPlayerSettings", storages = [Storage("MusicPlayer.xml")])
class MusicPlayerSettings : PersistentStateComponent<MusicPlayerSettings> {
    var musicFolder: String? = null
    var currentMusic: File? = null
    var cyclePlay: Boolean = false

    override fun getState(): MusicPlayerSettings {
        return this
    }

    override fun loadState(state: MusicPlayerSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
