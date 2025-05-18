package com.xxyxxdmc.hoshisuki

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.xxyxxdmc.ui.icons.MusicIcons
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
import javax.sound.sampled.*
import javax.swing.*
import kotlin.collections.ArrayList
import kotlin.math.floor


class HoshisukiUI : JPanel() {
    private val selectButton = JButton("")
    private var playButton = JButton("Folder")
    private val nextButton = JButton("Next")
    private val prevButton = JButton("Prev")
    private var playCase = JButton("")
    private val folderLabel = JLabel("Not choose folder")
    private val state = HoshisukiSettings.instance
    private var scrollPane: Component? = null
    private var musicFiles = ArrayList<File>()
    private val listModel = DefaultListModel<File>()
    private val listModelName = DefaultListModel<String>()
    private val list = JBList(listModelName)
    private var player: AdvancedPlayer? = null
    private var isPlaying = false
    private var playThread: Thread? = null
    private val clip: Clip = AudioSystem.getClip()
    private var selectedMusic: File? = null
    private var objectivePause = false


    init {
        selectButton.icon = MusicIcons.folder
        nextButton.icon = MusicIcons.playForward
        prevButton.icon = MusicIcons.playBack
        if (isPlaying) {
            playButton = JButton("Pause")
            playButton.icon = MusicIcons.pause
        } else {
            playButton = JButton("Play")
            playButton.icon = MusicIcons.run
        }
        when (state.playCase) {
            0 -> playCase = JButton("List Cycle")
            1 -> playCase = JButton("Alone Cycle")
            2 -> playCase = JButton("Order")
            3 -> playCase = JButton("Reverse Order")
            4 -> playCase = JButton("Random")
            5 -> playCase = JButton("Pause on Finish")
        }
        layout = BorderLayout()
        val controlPanel = JPanel().apply {
            add(prevButton)
            add(playButton)
            add(nextButton)
            add(playCase)
        }

        val displayPanel = JPanel().apply {
            layout = BorderLayout()
            add(JLabel("  Music Folder:  "), BorderLayout.WEST)
            add(folderLabel, BorderLayout.CENTER)
            folderLabel.text = state.musicFolder ?: "Not choose folder"
            add(selectButton, BorderLayout.EAST)
        }

        add(displayPanel, BorderLayout.NORTH)
        add(controlPanel, BorderLayout.SOUTH)

        if (state.musicFolder!=null) {
            displayMusicList(File(state.musicFolder!!))
        }
        if (musicFiles.isNotEmpty()) {
            listModel.clear()
            listModelName.clear()
            musicFiles.forEach { listModel.addElement(it) }
            musicFiles.forEach { listModelName.addElement(it.name) }
            scrollPane = JBScrollPane(list).apply {
                preferredSize = Dimension(200, 150)
            }
            list.addListSelectionListener {
                if (!it.valueIsAdjusting) {
                    selectedMusic = listModel.elementAt(list.selectedIndex)
                }
            }
        }
        selectButton.addActionListener { chooseFolder() }
        playButton.addActionListener {
            objectivePause = true
            state.currentMusic = selectedMusic
            list.isEnabled = !isPlaying
            playMusic()
            objectivePause = false
        }
        prevButton.addActionListener {
            if (state.musicFolder != null && isPlaying) {
                if (musicFiles.size > 1) {
                    objectivePause = true
                    var index = musicFiles.indexOf(state.currentMusic) - 1
                    if (index<0) index = musicFiles.size - 1
                    pauseMusic()
                    state.currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    playMusic()
                    objectivePause = false
                }
            }
        }
        nextButton.addActionListener {
            if (state.musicFolder != null && isPlaying) {
                if (musicFiles.size > 1) {
                    objectivePause = true
                    var index = musicFiles.indexOf(state.currentMusic) + 1
                    if (index >= musicFiles.size) index = 0
                    pauseMusic()
                    state.currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    playMusic()
                    objectivePause = false
                }
            }
        }
        playCase.addActionListener {
            if (state.playCase + 1 > 5) state.playCase = 0
            else state.playCase++
            val index = state.playCase
            when (index) {
                0 -> playCase.text = "List Cycle"
                1 -> playCase.text = "Alone Cycle"
                2 -> playCase.text = "Order"
                3 -> playCase.text = "Reverse Order"
                4 -> playCase.text = "Random"
                5 -> playCase.text = "Pause on Finish"
            }
            revalidate()
            repaint()
        }

        if (scrollPane!=null){
            scrollPane?.let { add(it, BorderLayout.CENTER) }
        }
    }

    private fun chooseFolder() {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            state.musicFolder = chooser.selectedFile.absolutePath
            displayMusicList(chooser.selectedFile)
        }
    }

    private fun displayMusicList(selectedFile: File) {
        val musicList = ArrayList<File>()
        if (selectedFile.listFiles()?.size !=0) {
            var haveMusic = false
            for (file in selectedFile.listFiles()!!) {
                if (file.extension.lowercase(Locale.getDefault()) in listOf("mp3", "wav", "aif", "aiff", "au")) {
                    haveMusic = true
                    break
                }
            }
            if (!haveMusic) {
                JOptionPane.showMessageDialog(null, "Don't have any supported music")
                return
            }
            folderLabel.text = selectedFile.path
            selectedFile.listFiles()?.forEach {
                if (it.extension.lowercase(Locale.getDefault()) in listOf("mp3", "wav", "aif", "aiff", "au")) {
                    musicList.add(it)
                }
            }
            musicFiles = musicList
            if (musicFiles.isNotEmpty()) {
                listModel.clear()
                listModelName.clear()
                musicFiles.forEach { listModel.addElement(it) }
                musicFiles.forEach { listModelName.addElement(it.name) }
                scrollPane = JBScrollPane(list).apply {
                    preferredSize = Dimension(200, 150)
                }
                list.selectionMode = ListSelectionModel.SINGLE_SELECTION
                list.addListSelectionListener {
                    if (!it.valueIsAdjusting) {
                        selectedMusic = listModel.elementAt(list.selectedIndex)
                        HoshisukiSettings().loadState(state)
                    }
                }
            }
            if (scrollPane!=null){
                scrollPane?.let { add(it, BorderLayout.CENTER) }
            }
            revalidate()
            repaint()
        } else {
            JOptionPane.showMessageDialog(null, "Empty Folder")
        }
    }
    private fun playMusic() {
        if (!isPlaying && selectedMusic != null) {
            try {
                when (state.currentMusic!!.extension.lowercase(Locale.getDefault())) {
                    "mp3" -> {
                        val fileStream = state.currentMusic?.let { FileInputStream(it) }
                        player = AdvancedPlayer(fileStream)
                        playThread = Thread {
                            try {
                                player?.play()
                            } catch (e: JavaLayerException) {
                                println("Play ERROR: ${e.message}")
                            }
                        }
                        player!!.playBackListener = object: PlaybackListener() {
                            override fun playbackFinished(evt: PlaybackEvent?) {
                                pauseMusic()
                                playCase()
                            }
                        }
                        playThread?.start()
                        isPlaying = true
                        playButton.text = "Pause"
                        playButton.icon = MusicIcons.pause
                    }
                    else -> {
                        val audioStream = state.currentMusic?.let { AudioSystem.getAudioInputStream(it) }
                        clip.open(audioStream)
                        clip.start()
                        clip.addLineListener { event: LineEvent ->
                            if (event.type === LineEvent.Type.STOP && !objectivePause) {
                                pauseMusic()
                                playCase()
                            }
                        }
                        isPlaying = true
                        playButton.text = "Pause"
                        playButton.icon = MusicIcons.pause
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
        if (player != null) player!!.close()
        clip.close()
        isPlaying = false
        playButton.text = "Play"
        playButton.icon = MusicIcons.run
        revalidate()
        repaint()
    }

    private fun playCase() {
        when (state.playCase) {
            0 -> {
                if (state.musicFolder != null && isPlaying) {
                    if (musicFiles.size > 1) {
                        var index = musicFiles.indexOf(state.currentMusic) + 1
                        if (index >= musicFiles.size) index = 0
                        state.currentMusic = musicFiles[index]
                        selectedMusic = musicFiles[index]
                        list.selectedIndex = index
                        playMusic()
                    }
                }
            }
            1 -> {
                if (state.currentMusic != null) {
                    playMusic()
                }
            }
            2 -> {
                if (state.musicFolder != null && isPlaying) {
                    if (musicFiles.size > 1) {
                        val index = musicFiles.indexOf(state.currentMusic) + 1
                        if (index >= musicFiles.size) return
                        state.currentMusic = musicFiles[index]
                        selectedMusic = musicFiles[index]
                        list.selectedIndex = index
                        playMusic()
                    }
                }
            }
            3 -> {
                if (state.musicFolder != null && isPlaying) {
                    if (musicFiles.size > 1) {
                        var index = musicFiles.indexOf(state.currentMusic) - 1
                        if (index < 0) index = musicFiles.size - 1
                        state.currentMusic = musicFiles[index]
                        selectedMusic = musicFiles[index]
                        list.selectedIndex = index
                        playMusic()
                    }
                }
            }
            4 -> {
                if (state.musicFolder != null) {
                    if (musicFiles.size > 1) {
                        var index = floor(musicFiles.indexOf(state.currentMusic) * Math.random()).toInt()
                        if (index<0) index = musicFiles.size - 1
                        pauseMusic()
                        state.currentMusic = musicFiles[index]
                        selectedMusic = musicFiles[index]
                        playMusic()
                    } else {
                        playMusic()
                    }
                }
            }
            5 -> {}
        }
    }
}
