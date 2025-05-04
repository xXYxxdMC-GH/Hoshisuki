package com.xxyxxdmc.musicplayer.ui

import javazoom.jl.player.Player
import java.awt.BorderLayout
import java.io.File
import java.io.FileInputStream
import javax.swing.*

class MusicPlayerUI : JPanel() {
    private var isPaused = false;
    private val selectButton = JButton("选择文件夹")
    private var playButton = JButton(if (isPaused) "暂停" else "播放")
    private val nextButton = JButton("下一首")
    private val prevButton = JButton("上一首")
    private val folderLabel = JLabel("未选择文件夹")
    var player : Player? = null
    var lastPosition = 0

    var currentMusic: File? = null

    init {
        layout = BorderLayout()
        val controlPanel = JPanel().apply {
            add(prevButton)
            add(playButton)
            add(nextButton)
        }

        add(selectButton, BorderLayout.NORTH)
        add(folderLabel, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.SOUTH)

        selectButton.addActionListener { chooseFolder() }
        playButton.addActionListener { playMusic() }
    }

    private fun chooseFolder() {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            folderLabel.text = chooser.selectedFile.absolutePath
            val musicList = ArrayList<String>()
            if (chooser.selectedFile.listFiles()?.size !=0) {
                currentMusic = chooser.selectedFile.listFiles()?.firstOrNull { it.extension in listOf("mp3", "wav", "ogg") }
                System.out.println(currentMusic)
                chooser.selectedFile.listFiles()?.forEach {
                    if (it.extension in listOf("mp3", "wav", "ogg")) musicList.add(it.name)
                }
                remove(folderLabel)
                val playlistUI = PlaylistUI()
                playlistUI.musicFiles = musicList
                playlistUI.init()
                add(playlistUI, BorderLayout.CENTER)
            } else {
                JOptionPane.showMessageDialog(null, "空文件夹")
            }
        }
    }

    fun playMusic() {
        Thread {
            try {
                val fis = currentMusic?.let { FileInputStream(it) }
                player = Player(fis)

                if (isPaused) {
                    player?.play(lastPosition)
                    isPaused = false
                } else {
                    pauseMusic()
                }
                repaint()
            } catch (e: Exception) {
                println("播放失败: ${e.message}")
            }
        }.start()
    }

    private fun pauseMusic() {
        if (player != null) {
            lastPosition = player!!.position
            player!!.close()
            isPaused = true
        }
    }
}
