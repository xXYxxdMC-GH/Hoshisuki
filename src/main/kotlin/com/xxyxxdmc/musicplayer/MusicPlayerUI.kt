package com.xxyxxdmc.musicplayer

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.advanced.AdvancedPlayer
import javazoom.jl.player.advanced.PlaybackEvent
import javazoom.jl.player.advanced.PlaybackListener
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.swing.*


class MusicPlayerUI : JPanel() {
    private val selectButton = JButton("选择文件夹")
    private var playButton = JButton("")
    private val nextButton = JButton("下一首")
    private val prevButton = JButton("上一首")
    private val folderLabel = JLabel("未选择文件夹")
    private val state = ApplicationManager.getApplication().getService(MusicPlayerSettings::class.java)
    private var scrollPane: Component? = null
    private var musicFiles: ArrayList<File>? = null
    private val listModel = DefaultListModel<File>()
    private val listModelName = DefaultListModel<String>()
    private val list = JBList(listModelName)
    private var player: AdvancedPlayer? = null
    private var isPlaying = false
    private var playThread: Thread? = null
    private val clip: Clip = AudioSystem.getClip()

    init {
        playButton = if (isPlaying) {
            JButton("暂停")
        } else {
            JButton("播放")
        }
        layout = BorderLayout()
        val controlPanel = JPanel().apply {
            add(prevButton)
            add(playButton)
            add(nextButton)
        }

        add(selectButton, BorderLayout.NORTH)
        add(controlPanel, BorderLayout.SOUTH)

        if (state.musicFolder!=null) {
            displayMusicList(File(state.musicFolder!!))
        } else {
            add(folderLabel, BorderLayout.CENTER)
        }
        if (!musicFiles.isNullOrEmpty()) {
            listModel.clear()
            listModelName.clear()
            musicFiles?.forEach { listModel.addElement(it) }
            musicFiles?.forEach { listModelName.addElement(it.name) }
            scrollPane = JBScrollPane(list).apply {
                preferredSize = Dimension(200, 150)
            }
            list.addListSelectionListener {
                if (!it.valueIsAdjusting) {
                    state.currentMusic = listModel.elementAt(list.selectedIndex)
                }
            }
        }
        selectButton.addActionListener { chooseFolder() }
        playButton.addActionListener { playMusic() }
        prevButton.addActionListener {

        }

        if (scrollPane!=null){
            scrollPane?.let { add(it, BorderLayout.CENTER) }
        }
    }

    private fun chooseFolder() {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            state.musicFolder = chooser.selectedFile.absolutePath
            MusicPlayerSettings().loadState(state)
            displayMusicList(chooser.selectedFile)
        }
    }

    private fun displayMusicList(selectedFile: File) {
        val musicList = ArrayList<File>()
        if (selectedFile.listFiles()?.size !=0) {
            var haveMusic = false
            for (file in selectedFile.listFiles()!!) {
                if (file.extension.lowercase(Locale.getDefault()) in listOf("mp3", "wav", "aif", "aiff", "au", "ogg")) {
                    haveMusic = true
                    break
                }
            }
            if (!haveMusic) {
                JOptionPane.showMessageDialog(null, "无支持的音乐")
                return
            }
            selectedFile.listFiles()?.forEach {
                if (it.extension.lowercase(Locale.getDefault()) in listOf("mp3", "wav", "aif", "aiff", "au", "ogg")) {
                    musicList.add(it)
                }
            }
            remove(folderLabel)
            musicFiles = musicList
            if (!musicFiles.isNullOrEmpty()) {
                listModel.clear()
                listModelName.clear()
                musicFiles?.forEach { listModel.addElement(it) }
                musicFiles?.forEach { listModelName.addElement(it.name) }
                scrollPane = JBScrollPane(list).apply {
                    preferredSize = Dimension(200, 150)
                }
                list.selectionMode = ListSelectionModel.SINGLE_SELECTION
                list.addListSelectionListener {
                    if (!it.valueIsAdjusting) {
                        state.currentMusic = listModel.elementAt(list.selectedIndex)
                        MusicPlayerSettings().loadState(state)
                    }
                }
            }
            if (scrollPane!=null){
                scrollPane?.let { add(it, BorderLayout.CENTER) }
            }
            revalidate()
            repaint()
        } else {
            JOptionPane.showMessageDialog(null, "空文件夹")
        }
    }
    private fun playMusic() {
        if (!isPlaying && state.currentMusic != null) {
            try {
                when (state.currentMusic!!.extension.lowercase(Locale.getDefault())) {
                    "mp3" -> {
                        val fileStream = state.currentMusic?.let { FileInputStream(it) }
                        player = AdvancedPlayer(fileStream)
                        playThread = Thread {
                            try {
                                player?.play()
                            } catch (e: JavaLayerException) {
                                println("播放错误: ${e.message}")
                            }
                        }
                        player!!.playBackListener = object: PlaybackListener() {
                            override fun playbackFinished(evt: PlaybackEvent?) {
                                player!!.close()
                                pauseMusic()
                            }
                        }
                        playThread?.start()
                        isPlaying = true
                        playButton.text = "暂停"
                    }
                    else -> {
                        val audioStream = state.currentMusic?.let { AudioSystem.getAudioInputStream(it) }
                        clip.open(audioStream)
                        clip.start()
                        clip.addLineListener { event: LineEvent ->
                            if (event.type === LineEvent.Type.STOP) {
                                clip.close()
                                pauseMusic()
                            }
                        }
                        isPlaying = true
                        playButton.text = "暂停"
                    }
                }
                revalidate()
                repaint()
            } catch (_: Exception) {}
        } else {
            pauseMusic()
        }
    }

    private fun pauseMusic() {
        if (player != null) {
            player?.close()
        }
        clip.close()
        isPlaying = false
        playButton.text = "播放"
        revalidate()
        repaint()
    }
}
