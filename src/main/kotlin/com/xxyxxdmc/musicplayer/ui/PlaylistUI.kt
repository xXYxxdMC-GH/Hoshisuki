package com.xxyxxdmc.musicplayer.ui

import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane

class PlaylistUI : JPanel() {
    var musicFiles: ArrayList<String>? = null
    private val listModel = DefaultListModel<String>()
    private val list = JBList(listModel)

    fun init() {
        layout = BorderLayout()
        if (!musicFiles.isNullOrEmpty()) {
            musicFiles?.forEach { listModel.addElement(it) }
            val scrollPane = JScrollPane(list).apply {
                preferredSize = Dimension(200, 150)
            }
            add(scrollPane, BorderLayout.CENTER)
        }
    }
}
