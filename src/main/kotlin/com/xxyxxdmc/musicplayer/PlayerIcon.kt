package com.xxyxxdmc.musicplayer

import com.intellij.openapi.util.IconLoader.getIcon
import javax.swing.Icon


interface PlayerIcon {
    var PLAYER: Icon
        get() = getIcon("/icons/player.svg", PlayerIcon::class.java)
        set(value) = TODO()
}