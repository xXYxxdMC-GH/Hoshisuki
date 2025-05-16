package com.xxyxxdmc.hoshisuki

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.File

@State(name = "HoshisukiSettings", storages = [Storage("HoshisukiSettings.xml")])
class HoshisukiSettings : PersistentStateComponent<HoshisukiSettings> {
    var musicFolder: String? = null
    var currentMusic: File? = null
    var playCase: Int = 0

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
