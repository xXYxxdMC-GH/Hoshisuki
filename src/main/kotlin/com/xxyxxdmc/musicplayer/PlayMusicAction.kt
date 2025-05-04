package com.xxyxxdmc.musicplayer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.xxyxxdmc.musicplayer.ui.MusicPlayerUI
import javax.swing.JOptionPane

class PlayMusicAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val file = MusicPlayerUI().currentMusic
        if (file != null) {
            Thread {
                try {
                    MusicPlayerUI().playMusic()
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(null, "����ʧ��: ${e.message}")
                }
            }.start()
        } else {
            JOptionPane.showMessageDialog(null, "����ѡ�������ļ���")
        }
    }
}