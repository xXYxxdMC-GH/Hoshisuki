package com.xxyxxdmc.hoshisuki

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

@State(name = "HoshisukiSettings", storages = [Storage("HoshisukiSettings.xml")])
class HoshisukiSettings : PersistentStateComponent<HoshisukiSettings> {
    var musicFolder: String? = null
    var playCase: Int = 0
    var likeList = ArrayList<String>()
    var dislikeList = ArrayList<String>()
    var detailTooltip: Boolean = true
    var alonePlayTimes: Int = 5
    var sensitiveIcon: Boolean = false
    var likeWeight: Double = 0.0
    var dislikeWeight: Double = 0.0
    var antiAgainLevel: Int = 0
    var optimizePanel: Boolean = false
    var musicCoverMap = mutableMapOf<String, String>()
    var beautifyTitle: Int = 0
    var beautifyTitleEnabled: Boolean = false
    var musicFolderList = ArrayList<String>()

    @Nullable
    override fun getState(): HoshisukiSettings {
        return this
    }

    override fun loadState(@NotNull state: HoshisukiSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        val instance: HoshisukiSettings
            get() = ApplicationManager.getApplication().getService(HoshisukiSettings::class.java)
    }

}
