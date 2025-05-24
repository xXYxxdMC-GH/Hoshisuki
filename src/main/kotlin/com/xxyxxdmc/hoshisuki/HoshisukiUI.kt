package com.xxyxxdmc.hoshisuki

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.xxyxxdmc.ui.component.IconTooltipActionButton
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


@Suppress("DialogTitleCapitalization")
class HoshisukiUI : JPanel() {
    private val bundle = HoshisukiBundle
    private val selectButton = JButton("Choose")
    private var playButton = IconTooltipActionButton(MusicIcons.run, bundle.message("button.play.tooltip")) {}
    private val nextButton = IconTooltipActionButton(MusicIcons.playForward, "Next") {}
    private val prevButton = IconTooltipActionButton(MusicIcons.playBack, "Prev") {}
    private var playCase = IconTooltipActionButton(MusicIcons.listCycle, "") {}
    private val folderLabel = JLabel("Not choose folder")
    private val state = HoshisukiSettings.instance
    private var scrollPane: Component? = null
    private var musicFiles = ArrayList<File>()
    private val listModel = DefaultListModel<File>()
    private val listModelPanel = DefaultListModel<JPanel>()
    private val list = JBList(listModelPanel)
    private var player: AdvancedPlayer? = null
    private var isPlaying = false
    private var playThread: Thread? = null
    private val clip: Clip = AudioSystem.getClip()
    private var selectedMusic: File? = null
    private var objectivePause = false
    private var currentMusic: File? = null
    private var currentLikeList = ArrayList<File>()
    private var currentDislikeList = ArrayList<File>()
    private var currentNormalList = ArrayList<File>()

    init {
        //minimumSize = Dimension()
        selectButton.icon = MusicIcons.folder
        playButton.action = Runnable {
            if (state.musicFolder != null) {
                objectivePause = true
                currentMusic = if (selectedMusic != null) {
                    selectedMusic
                } else if (musicFiles.isNotEmpty()) {
                    list.selectedIndex = 0
                    musicFiles[0]
                } else {
                    null
                }

                if (currentMusic != null) {
                    refreshPlayingIconInList()
                    playMusic()
                }
                objectivePause = false
            }
        }
        when (state.playCase) {
            0 -> {
                playCase.text = "List Cycle"
                playCase.icon = MusicIcons.listCycle
            }
            1 -> {
                playCase.text = "Alone Reverse Cycle"
                playCase.icon = MusicIcons.aloneCycle
            }
            2 -> {
                playCase = JButton("Order")
                playCase.icon = MusicIcons.listPlay
            }
            3 -> {
                playCase = JButton("Reverse Order")
                playCase.icon = MusicIcons.listReversePlay
            }
            4 -> {
                playCase = JButton("Random")
                playCase.icon = MusicIcons.random
            }
            5 -> {
                playCase = JButton("Stop on Finish")
                playCase.icon = MusicIcons.pauseOnFinish
            }
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
            add(JLabel("  "+"Music Folder:"+"  "), BorderLayout.WEST)
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
            listModelPanel.clear()
            musicFiles.forEach { listModel.addElement(it) }
            musicFiles.forEach { listModelPanel.addElement(createMusicPanel(it)) }
            scrollPane?.let { remove(it) }
            scrollPane = JBScrollPane(list).apply {
                preferredSize = Dimension(200, 150)
            }
            list.cellRenderer = PanelCellRenderer()
            list.selectionMode = ListSelectionModel.SINGLE_SELECTION
            list.addListSelectionListener {
                if (!it.valueIsAdjusting) {
                    selectedMusic = listModel.elementAt(list.selectedIndex)
                }
            }
        }
        selectButton.addActionListener { chooseFolder() }

        prevButton.addActionListener {
            if (state.musicFolder != null && isPlaying) {
                if (musicFiles.size > 1) {
                    objectivePause = true
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1 && musicFiles.isNotEmpty()) {
                        index = 0
                    } else if (index == -1) {
                        objectivePause = false
                        return@addActionListener
                    }

                    index--
                    if (index < 0) index = musicFiles.size - 1

                    stopMusic()
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
                    playMusic()
                    objectivePause = false
                }
            }
        }

        nextButton.addActionListener {
            if (state.musicFolder != null && isPlaying) {
                if (musicFiles.size > 1) {
                    objectivePause = true
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1 && musicFiles.isNotEmpty()) {
                        index = 0
                    } else if (index == -1) {
                        objectivePause = false
                        return@addActionListener
                    }

                    index++
                    if (index >= musicFiles.size) index = 0

                    stopMusic()
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
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
                // TODO: Add More Case
                0 -> {
                    playCase.text = "List Cycle"
                    playCase.icon = MusicIcons.listCycle
                }
                1 -> {
                    playCase.text = "Alone Cycle"
                    playCase.icon = MusicIcons.aloneCycle
                }
                2 -> {
                    playCase.text = "Order"
                    playCase.icon = MusicIcons.listPlay
                }
                3 -> {
                    playCase.text = "Reverse Order"
                    playCase.icon = MusicIcons.listReversePlay
                }
                4 -> {
                    playCase.text = "Random"
                    playCase.icon = MusicIcons.random
                }
                5 -> {
                    playCase.text = "Stop on Finish"
                    playCase.icon = MusicIcons.pauseOnFinish
                }
            }
            revalidate()
            repaint()
        }

        if (scrollPane!=null){
            scrollPane?.let { add(it, BorderLayout.CENTER) }
        }
    }

    private fun refreshPlayingIconInList() {
        if (listModelPanel.isEmpty || listModel.isEmpty || listModelPanel.size() != listModel.size()) {
            return
        }

        for (i in 0 until listModelPanel.size()) {
            val panel = listModelPanel.getElementAt(i) ?: continue
            val fileForPanel = listModel.getElementAt(i) ?: continue

            panel.removeAll()

            if (fileForPanel.absolutePath == currentMusic?.absolutePath) {
                panel.add(JLabel().apply { icon = MusicIcons.playing }, BorderLayout.WEST)
            }
            panel.add(JLabel(" " + fileForPanel.name), BorderLayout.CENTER)

            panel.revalidate()
        }
        list.repaint()
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
            selectedFile.listFiles()?.forEach {
                if (it.extension.lowercase(Locale.getDefault()) in listOf("mp3", "wav", "aif", "aiff", "au")) {
                    musicList.add(it)
                }
            }
            if (musicList.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Don't have any supported music")
                return
            }
            folderLabel.text = selectedFile.path

            musicFiles = musicList
            if (musicFiles.isNotEmpty()) {
                listModel.clear()
                listModelPanel.clear()
                musicFiles.forEach { listModel.addElement(it) }
                musicFiles.forEach { listModelPanel.addElement(createMusicPanel(it)) }
                scrollPane?.let { remove(it) }
                scrollPane = JBScrollPane(list).apply {
                    preferredSize = Dimension(200, 150)
                }
                list.cellRenderer = PanelCellRenderer()
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
    private fun createMusicPanel(music: File): JPanel {
        return JPanel().apply {
            layout = BorderLayout()
            if (music === currentMusic) {
                add(JLabel().apply { icon = MusicIcons.playing }, BorderLayout.WEST)
            }
            add(JLabel(" " + music.name), BorderLayout.CENTER)
        }
    }
    private fun playMusic() {
        if (!isPlaying && selectedMusic != null) {
            try {
                when (currentMusic!!.extension.lowercase(Locale.getDefault())) {
                    "mp3" -> {
                        val fileStream = currentMusic?.let { FileInputStream(it) }
                        player = AdvancedPlayer(fileStream)
                        if (playThread != null) if (playThread!!.isAlive) playThread!!.interrupt()
                        playThread = Thread {
                            try {
                                player!!.play()
                            } catch (e: JavaLayerException) {
                                println("Play ERROR: ${e.message}")
                            }
                        }
                        player!!.playBackListener = object: PlaybackListener() {
                            override fun playbackFinished(evt: PlaybackEvent?) {
                                stopMusic()
                                playCase()
                            }
                        }
                        playThread!!.start()
                        isPlaying = true
                        playButton.text = "Stop"
                        playButton.icon = MusicIcons.stop
                    }
                    else -> {
                        val audioStream = currentMusic?.let { AudioSystem.getAudioInputStream(it) }
                        clip.open(audioStream)
                        clip.start()
                        clip.addLineListener { event: LineEvent ->
                            if (event.type === LineEvent.Type.STOP && !objectivePause) {
                                stopMusic()
                                playCase()
                            }
                        }
                        isPlaying = true
                        playButton.text = "Stop"
                        playButton.icon = MusicIcons.stop
                    }
                }
                revalidate()
                repaint()
            } catch (_: Exception) {}
        } else {
            stopMusic()
        }
    }

    private fun stopMusic() {
        if (player != null && playThread != null) {
            player!!.close()
            if (playThread!!.isAlive) playThread!!.interrupt()
        }
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
                if (musicFiles.isNotEmpty()) {
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) index = -1
                    index++
                    if (index >= musicFiles.size) index = 0
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            1 -> {
                if (currentMusic != null) {
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            2 -> {
                if (musicFiles.isNotEmpty()) {
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) {
                        return
                    }
                    index++
                    if (index >= musicFiles.size) {
                        currentMusic = null
                        refreshPlayingIconInList()
                        return
                    }
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            3 -> {
                if (musicFiles.isNotEmpty()) {
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) {
                        return
                    }
                    index--
                    if (index < 0) {
                        currentMusic = null
                        refreshPlayingIconInList()
                        return
                    }
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            4 -> {
                if (musicFiles.isNotEmpty()) {
                    var newIndex: Int
                    if (musicFiles.size > 1) {
                        val currentIndex = musicFiles.indexOf(currentMusic)
                        do {
                            newIndex = Random().nextInt(musicFiles.size)
                        } while (newIndex == currentIndex)
                    } else {
                        newIndex = 0
                    }
                    currentMusic = musicFiles[newIndex]
                    selectedMusic = musicFiles[newIndex]
                    list.selectedIndex = newIndex
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            5 -> {
                refreshPlayingIconInList()
            }
        }
    }
}

