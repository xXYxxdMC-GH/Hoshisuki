package com.xxyxxdmc.musicplayer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import javax.swing.JOptionPane

class PlayMusicAction : AnAction() {
    private val state = ApplicationManager.getApplication().getService(MusicPlayerSettings::class.java)
    override fun actionPerformed(event: AnActionEvent) {
        val file = state.currentMusic
        if (file != null) {
            Thread {
                try {
                    MyMusicPlugin.musicUI.playMusic()
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(null, "播放失败: ${e.message}")
                }
            }.start()
        } else {
            JOptionPane.showMessageDialog(null, "请先选择音乐文件夹")
        }
    }
}