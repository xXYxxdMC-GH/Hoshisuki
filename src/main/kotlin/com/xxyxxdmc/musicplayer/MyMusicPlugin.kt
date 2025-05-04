package com.xxyxxdmc.musicplayer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.WindowManager
import com.xxyxxdmc.musicplayer.ui.MusicPlayerUI
import java.awt.Dimension
import javax.swing.JFrame

class MyMusicPlugin : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val frame = JFrame("IntelliJ Music Player").apply {
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            size = Dimension(400, 300)
            add(MusicPlayerUI())
            isVisible = true
            setLocationRelativeTo(WindowManager.getInstance().findVisibleFrame())
        }
    }
}