package com.xxyxxdmc.hoshisuki

import com.intellij.ide.HelpTooltip
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.xxyxxdmc.RandomPlayException
import com.xxyxxdmc.component.CoverPanel
import com.xxyxxdmc.component.IconTooltipActionButton
import com.xxyxxdmc.icons.MusicIcons
import com.xxyxxdmc.player.OggPlaybackListener
import com.xxyxxdmc.player.OggPlayer
import com.xxyxxdmc.player.OggPlayerException
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.advanced.AdvancedPlayer
import javazoom.jl.player.advanced.PlaybackEvent
import javazoom.jl.player.advanced.PlaybackListener
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent
import javax.swing.*
import kotlin.math.floor
import kotlin.math.round

class HoshisukiUI : JPanel() {
    private val bundle = HoshisukiBundle
    init {
        try {
            AudioSystem.getClip()
        } catch (_ : Exception) {
            JOptionPane.showMessageDialog(null,
                bundle.message("message.no.audio.device"),
                bundle.message("message.no.audio.device.title"),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    private val state = HoshisukiSettings.instance
    private fun getExplainableMessage(key: String): String {
        return bundle.message(if (state.detailTooltip) "$key.detail" else key)
    }
    private val selectButton = JButton(bundle.message("button.choose.text")).apply { icon = MusicIcons.folder }
    private var likeButton = IconTooltipActionButton(MusicIcons.like, getExplainableMessage("button.like.tooltip")) {}
    private var playButton = IconTooltipActionButton(MusicIcons.run, getExplainableMessage("button.play.tooltip")) {}
    private val nextButton = IconTooltipActionButton(MusicIcons.playForward, getExplainableMessage("button.next.tooltip")) {}
    private val prevButton = IconTooltipActionButton(MusicIcons.playBack, getExplainableMessage("button.prev.tooltip")) {}
    private val rescanButton = IconTooltipActionButton(MusicIcons.rescan, getExplainableMessage("button.rescan.tooltip")) {}
    private var playCase = IconTooltipActionButton(MusicIcons.listCycle, "") {}
    private val folderLabel = JLabel(bundle.message("folder.label.not.chosen"))
    private val coverPanel = CoverPanel(null, -1, -1)
    private var scrollPane: Component? = null
    private var settingPanel: JPanel = JPanel()
    private var defaultSettingHeight: Int = 0
    private var musicFiles = ArrayList<File>()
    private val listModel = DefaultListModel<File>()
    private val listModelPanel = DefaultListModel<JPanel>()
    private val list = JBList(listModelPanel)
    private var player: AdvancedPlayer? = null
    private val clip: Clip = AudioSystem.getClip()
    private val oggPlayer: OggPlayer = OggPlayer()
    private var isPlaying = false
    private var mp3PlayThread: Thread? = null
    private var oggPlayThread: Thread? = null
    private var selectedMusic: File? = null
    private var objectivePause = false
    private var currentMusic: File? = null
    private var currentLikeList = ArrayList<File>()
    private var currentDislikeList = ArrayList<File>()
    private var currentNormalList = ArrayList<File>()
    private var alonePlayTime = 0
    private var playedMusic = ArrayList<File>()

    init {
        isFocusable = true

        minimumSize = Dimension(150, 0)

        likeButton.action = Runnable {
            if (selectedMusic != null) {
                when (selectedMusic) {
                    in currentLikeList -> {
                        currentLikeList.remove(selectedMusic)
                        state.likeList.remove(selectedMusic!!.absolutePath)
                        currentDislikeList.add(selectedMusic!!)
                        state.dislikeList.add(selectedMusic!!.absolutePath)
                        likeButton.text = getExplainableMessage("button.un.dislike.tooltip")
                        likeButton.icon = MusicIcons.unDislike
                    }
                    in currentDislikeList -> {
                        currentDislikeList.remove(selectedMusic)
                        state.dislikeList.remove(selectedMusic!!.absolutePath)
                        likeButton.text = getExplainableMessage("button.like.tooltip")
                        likeButton.icon = MusicIcons.like
                    }
                    else -> {
                        currentLikeList.add(selectedMusic!!)
                        state.likeList.add(selectedMusic!!.absolutePath)
                        likeButton.text = getExplainableMessage("button.dislike.tooltip")
                        likeButton.icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
                    }
                }
                refreshPlayingIconInList()
                revalidate()
                repaint()
            }
        }

        playButton.action = Runnable {
            if (state.musicFolder != null) {
                objectivePause = true
                currentMusic = if (selectedMusic != null) {
                    selectedMusic
                } else if (musicFiles.isNotEmpty()) {
                    list.selectedIndex = 0
                    musicFiles[0]
                } else null

                playedMusic.clear()
                refreshLikeButtonVisuals()
                if (currentMusic != null) {
                    playMusic()
                }
                revalidate()
                repaint()
                objectivePause = false
            }
        }

        rescanButton.action = Runnable {
            if (state.musicFolder != null) {
                displayMusicList(File(state.musicFolder!!))
            }
        }

        layout = BorderLayout()

        when (state.playCase) {
            0 -> { // List Cycle
                playCase.text = getExplainableMessage("play.case.list.cycle")
                playCase.icon = MusicIcons.listCycle
            }
            1 -> { // List Reverse Cycle
                playCase.text = getExplainableMessage("play.case.list.reverse.cycle")
                playCase.icon = MusicIcons.listReverseCycle
            }
            2 -> { // Alone Cycle
                playCase.text = getExplainableMessage("play.case.alone.cycle")
                playCase.icon = MusicIcons.aloneCycle
            }
            3 -> { // Alone Finite Cycle
                playCase.text = getExplainableMessage("play.case.alone.finite.cycle")
                playCase.icon = MusicIcons.aloneCycleInTimes
            }
            4 -> { // List Play
                playCase.text = getExplainableMessage("play.case.order")
                playCase.icon = MusicIcons.listPlay
            }
            5 -> { // List Reverse Play
                playCase.text = getExplainableMessage("play.case.reverse.order")
                playCase.icon = MusicIcons.listReversePlay
            }
            6 -> { // Random
                playCase.text = getExplainableMessage("play.case.random")
                playCase.icon = MusicIcons.random
            }
            7 -> { // Random Finite
                playCase.text = getExplainableMessage("play.case.random.finite")
                playCase.icon = MusicIcons.randomInTimes
            }
            8 -> { // Stop on Finish
                playCase.text = getExplainableMessage("play.case.stop.on.finish")
                playCase.icon = MusicIcons.stopOnFinish
            }
        }

        val controlPanel = JPanel().apply {
            add(likeButton)
            add(prevButton)
            add(playButton)
            add(nextButton)
            add(playCase)
        }

        // 这一部分比较抽象，我将会逐条进行讲解
        settingPanel = JPanel().apply {
            //设置布局管理器
            layout = BoxLayout(this@apply, BoxLayout.PAGE_AXIS)
            //添加一条横线
            add(JSeparator(SwingConstants.HORIZONTAL))
            //添加空缺
            add(Box.createVerticalStrut(5))
            //添加详细信息显示面板
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.detail.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.detail.context")).installOn(this)
                }, BorderLayout.WEST)
                add(JBCheckBox("", state.detailTooltip).apply {
                    addActionListener {
                        state.detailTooltip = this.isSelected
                        refreshAllButtonTooltips()
                    } }, BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
            //添加反敏感面板
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.sensitive.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.sensitive.context")).installOn(this)
                }, BorderLayout.WEST)
                add(JBCheckBox("", state.sensitiveIcon).apply {
                    addActionListener {
                        state.sensitiveIcon = this.isSelected
                        if (likeButton.icon == MusicIcons.dislike && this.isSelected) {
                            likeButton.icon = MusicIcons.dislikeAnti
                        } else if (likeButton.icon == MusicIcons.dislikeAnti && !this.isSelected) {
                            likeButton.icon = MusicIcons.dislike
                        }
                        refreshPlayingIconInList()
                        revalidate()
                        repaint()
                    }
                }, BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
            //添加面板显示优化
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.optimize.panel.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.optimize.panel.context")).installOn(this)
                }, BorderLayout.WEST)
                add(JBCheckBox("", state.optimizePanel).apply {
                    addActionListener {
                        state.optimizePanel = this.isSelected
                    } }, BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
            //添加抗重复播放面板
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.anti.again.level.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.anti.again.level.context")).installOn(this)
                })
                val options = arrayOf(bundle.message("option.anti.again.level.off"), bundle.message("option.anti.again.level.normal"), bundle.message("option.anti.again.level.enhanced"))
                add(JComboBox(options).apply {
                    selectedIndex = state.antiAgainLevel
                    addItemListener { state.antiAgainLevel = it.stateChange }
                    preferredSize = Dimension(80, 30)
                }, BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
            //添加喜欢歌曲播放权重面板
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.weight.liked.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.weight.context")).installOn(this)
                })
                add(JSpinner(SpinnerNumberModel(state.likeWeight, -1.0, 1.0, 0.1).apply {
                    addChangeListener { state.likeWeight = value as Double }
                    preferredSize = Dimension(80, 30)
                }), BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
            //添加不喜欢歌曲播放权重面板
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.weight.disliked.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.weight.context")).installOn(this)
                })
                add(JSpinner(SpinnerNumberModel(state.dislikeWeight, -1.0, 1.0, 0.1).apply {
                    addChangeListener { state.dislikeWeight = value as Double }
                    preferredSize = Dimension(80, 30)
                }), BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
            //添加单曲循环次数面板
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.alone.play.times.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.alone.play.times.context")).installOn(this)
                })
                add(JSpinner(SpinnerNumberModel(state.alonePlayTimes, 2, Int.MAX_VALUE, 1).apply {
                    preferredSize = Dimension(80, 30)
                    addChangeListener { state.alonePlayTimes = value as Int }
                }), BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
        }

        val displayPanel = JPanel().apply {
            layout = BorderLayout()
            add(JLabel("  "+bundle.message("music.folder.label.prefix")+" "), BorderLayout.WEST)
            add(folderLabel, BorderLayout.CENTER)
            folderLabel.text = state.musicFolder ?: bundle.message("folder.label.not.chosen")
            add(JPanel().apply {
                layout = BorderLayout()
                add(rescanButton, BorderLayout.WEST)
                add(selectButton, BorderLayout.CENTER)
            }, BorderLayout.EAST)
        }

        add(JPanel().apply {
            layout = BorderLayout()
            add(coverPanel, BorderLayout.CENTER)
            add(JSeparator(SwingConstants.HORIZONTAL), BorderLayout.SOUTH)
            add(displayPanel, BorderLayout.SOUTH)
        }, BorderLayout.NORTH)

        add(JPanel().apply { layout = BorderLayout()
            add(controlPanel, BorderLayout.NORTH)
            add(settingPanel, BorderLayout.CENTER)
        }, BorderLayout.SOUTH)

        if (state.musicFolder!=null) {
            displayMusicList(File(state.musicFolder!!))
        }

        selectButton.addActionListener { chooseFolder() }

        prevButton.action = Runnable {
            if (state.musicFolder != null && isPlaying) {
                if (musicFiles.size > 1) {
                    if (prevButton.icon == MusicIcons.playBack) {
                        var index = musicFiles.indexOf(currentMusic)
                        index--
                        if (index < 0) index = musicFiles.size - 1
                        currentMusic = musicFiles[index]
                        selectedMusic = musicFiles[index]
                        list.selectedIndex = index
                        playMusic()
                    } else {
                        currentMusic = musicFiles.last()
                        selectedMusic = musicFiles.last()
                        list.selectedIndex = musicFiles.size - 1
                        playMusic()
                    }
                }
            }
        }

        nextButton.action = Runnable {
            if (state.musicFolder != null && isPlaying) {
                if (musicFiles.size > 1) {
                    if (nextButton.icon == MusicIcons.playForward) {
                        var index = musicFiles.indexOf(currentMusic)
                        if (index == -1) index = -1
                        index++
                        if (index >= musicFiles.size) index = 0
                        currentMusic = musicFiles[index]
                        selectedMusic = musicFiles[index]
                        list.selectedIndex = index
                        playMusic()
                    } else {
                        currentMusic = musicFiles.first()
                        selectedMusic = musicFiles.first()
                        list.selectedIndex = 0
                        playMusic()
                    }
                }
            }
        }

        playCase.action = Runnable {
            if (state.playCase + 1 > 8) state.playCase = 0
            else state.playCase++
            playedMusic.clear()
            when (state.playCase) {
                0 -> { // List Cycle
                    playCase.text = getExplainableMessage("play.case.list.cycle")
                    playCase.icon = MusicIcons.listCycle
                }
                1 -> { // List Reverse Cycle
                    playCase.text = getExplainableMessage("play.case.list.reverse.cycle")
                    playCase.icon = MusicIcons.listReverseCycle
                }
                2 -> { // Alone Cycle
                    playCase.text = getExplainableMessage("play.case.alone.cycle")
                    playCase.icon = MusicIcons.aloneCycle
                }
                3 -> { // Alone Finite Cycle
                    playCase.text = getExplainableMessage("play.case.alone.finite.cycle")
                    playCase.icon = MusicIcons.aloneCycleInTimes
                }
                4 -> { // List Play
                    playCase.text = getExplainableMessage("play.case.order")
                    playCase.icon = MusicIcons.listPlay
                }
                5 -> { // List Reverse Play
                    playCase.text = getExplainableMessage("play.case.reverse.order")
                    playCase.icon = MusicIcons.listReversePlay
                }
                6 -> { // Random
                    playCase.text = getExplainableMessage("play.case.random")
                    playCase.icon = MusicIcons.random
                }
                7 -> { // Random Finite
                    playCase.text = getExplainableMessage("play.case.random.finite")
                    playCase.icon = MusicIcons.randomInTimes
                }
                8 -> { // Stop on Finish
                    playCase.text = getExplainableMessage("play.case.stop.on.finish")
                    playCase.icon = MusicIcons.stopOnFinish
                }
            }
            revalidate()
            repaint()
        }

        if (scrollPane!=null){
            scrollPane?.let { add(it, BorderLayout.CENTER) }
        }

        addComponentListener(object : ComponentListener {
            override fun componentResized(e: ComponentEvent) {
                if (isPlaying) {
                    coverPanel.edgeLength = size.width
                    revalidate()
                    repaint()
                }
            }
            override fun componentMoved(e: ComponentEvent?) {
            }
            override fun componentShown(e: ComponentEvent?) {
            }
            override fun componentHidden(e: ComponentEvent?) {
            }
        })

//        addKeyListener( object: KeyListener {
//            override fun keyTyped(e: KeyEvent?) {
//            }
//            override fun keyPressed(e: KeyEvent?) {
//                if (e?.keyCode == KeyEvent.VK_SHIFT && prevButton.icon == MusicIcons.playBack) {
//                    prevButton.icon = MusicIcons.playFirst
//                    nextButton.icon = MusicIcons.playLast
//                    revalidate()
//                    repaint()
//                }
//            }
//            override fun keyReleased(e: KeyEvent?) {
//                if (e?.keyCode == KeyEvent.VK_SHIFT && prevButton.icon == MusicIcons.playFirst) {
//                    prevButton.icon = MusicIcons.playBack
//                    nextButton.icon = MusicIcons.playForward
//                    revalidate()
//                    repaint()
//                }
//            }
//        })
    }

    private fun refreshPlayCaseButtonVisuals() {
        val (textKey, icon) = when (state.playCase) {
            0 -> "play.case.list.cycle" to MusicIcons.listCycle
            1 -> "play.case.list.reverse.cycle" to MusicIcons.listReverseCycle
            2 -> "play.case.alone.cycle" to MusicIcons.aloneCycle
            3 -> "play.case.alone.finite.cycle" to MusicIcons.aloneCycleInTimes
            4 -> "play.case.order" to MusicIcons.listPlay
            5 -> "play.case.reverse.order" to MusicIcons.listReversePlay
            6 -> "play.case.random" to MusicIcons.random
            7 -> "play.case.random.finite" to MusicIcons.randomInTimes
            8 -> "play.case.stop.on.finish" to MusicIcons.stopOnFinish
            else -> "play.case.list.cycle" to MusicIcons.listCycle
        }
        playCase.text = getExplainableMessage(textKey)
        playCase.icon = icon
    }

    private fun refreshLikeButtonVisuals() {
        if (selectedMusic != null) {
            when {
                currentLikeList.contains(selectedMusic) -> {
                    likeButton.text = getExplainableMessage("button.dislike.tooltip")
                    likeButton.icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
                }
                currentDislikeList.contains(selectedMusic) -> {
                    likeButton.text = getExplainableMessage("button.un.dislike.tooltip")
                    likeButton.icon = MusicIcons.unDislike
                }
                else -> {
                    likeButton.text = getExplainableMessage("button.like.tooltip")
                    likeButton.icon = MusicIcons.like
                }
            }
        } else {
            likeButton.text = getExplainableMessage("button.like.tooltip")
            likeButton.icon = MusicIcons.like
        }
    }

    private fun refreshPlayingIconInList() {
        refreshLikeButtonVisuals()
        if (listModelPanel.isEmpty || listModel.isEmpty || listModelPanel.size() != listModel.size()) {
            return
        }

        for (i in 0 until listModelPanel.size()) {
            val panel = listModelPanel.getElementAt(i) ?: continue
            val fileForPanel = listModel.getElementAt(i) ?: continue

            if (state.optimizePanel) {
                var panelHasPlayingIcon = false
                var panelLikeStatusIcon: Icon? = null
                var panelHasCorrectNameLabel = false
                val expectedNameText = " " + fileForPanel.name

                panel.components.forEach { comp ->
                    if (comp is JLabel) {
                        if (comp.icon == MusicIcons.playing) {
                            panelHasPlayingIcon = true
                        } else if (comp.icon == MusicIcons.like || comp.icon == MusicIcons.dislike || comp.icon == MusicIcons.dislikeAnti) {
                            panelLikeStatusIcon = comp.icon
                        } else if (comp.text == expectedNameText && comp.icon == null) { // Name label usually has no icon
                            panelHasCorrectNameLabel = true
                        }
                    }
                }

                val shouldHavePlayingIcon = (fileForPanel === currentMusic && isPlaying)
                val expectedLikeStatusIcon: Icon? = when {
                    fileForPanel in currentLikeList -> MusicIcons.like
                    fileForPanel in currentDislikeList -> if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
                    else -> null
                }

                if (panelHasCorrectNameLabel &&
                    shouldHavePlayingIcon == panelHasPlayingIcon &&
                    expectedLikeStatusIcon == panelLikeStatusIcon) {
                    continue
                }
            }

            panel.removeAll()

            if (fileForPanel === currentMusic && isPlaying) {
                panel.add(JLabel().apply { icon = MusicIcons.playing }, BorderLayout.WEST)
            }
            panel.add(JLabel(" " + fileForPanel.name), BorderLayout.CENTER)

            if (fileForPanel in currentLikeList) {
                panel.add(JLabel().apply { icon = MusicIcons.like }, BorderLayout.EAST)
            } else if (fileForPanel in currentDislikeList) {
                panel.add(JLabel().apply { icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike }, BorderLayout.EAST)
            }

            panel.revalidate()
        }
        list.repaint()
    }

    private fun chooseFolder() {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            displayMusicList(chooser.selectedFile)
        }
    }

    private fun displayMusicList(selectedFile: File) {
        val musicList = ArrayList<File>()
        if (selectedFile.listFiles()?.isNotEmpty() == true) {
            selectedFile.listFiles()?.forEach {
                if (it.isFile && it.extension.lowercase(Locale.getDefault()) in listOf("mp3", "wav", "aif", "aiff", "au", "ogg")) {
                    musicList.add(it)
                }
            }
            if (musicList.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    bundle.message("message.no.supported.music"),
                    bundle.message("message.no.music.title"),
                    JOptionPane.INFORMATION_MESSAGE
                )
                return
            }
            state.musicFolder = selectedFile.absolutePath
            folderLabel.text = selectedFile.path

            currentMusic = null
            selectedMusic = null
            currentLikeList.clear()
            currentDislikeList.clear()
            currentNormalList.clear()
            musicFiles = musicList
            if (musicFiles.isNotEmpty()) {
                musicFiles.forEach {
                    when (it.absolutePath) {
                        in state.likeList -> currentLikeList.add(it)
                        in state.dislikeList -> currentDislikeList.add(it)
                        else -> currentNormalList.add(it)
                    }
                }
                listModel.clear()
                listModelPanel.clear()
                musicFiles.forEach { listModel.addElement(it) }
                musicFiles.forEach { listModelPanel.addElement(createMusicPanel(it)) }
                scrollPane?.let { remove(it) }
                list.cellRenderer = PanelCellRenderer()
                list.selectionMode = ListSelectionModel.SINGLE_SELECTION
                list.listSelectionListeners.forEach { list.removeListSelectionListener(it) }
                list.addListSelectionListener {
                    if (!it.valueIsAdjusting) {
                        if (list.selectedIndex != -1 && list.selectedIndex < listModel.size()) {
                            selectedMusic = listModel.elementAt(list.selectedIndex)
                            refreshLikeButtonVisuals()
                        }
                    }
                }
                scrollPane = JBScrollPane(list).apply {
                    preferredSize = Dimension(200, 150)
                }
            }
            if (scrollPane!=null){
                scrollPane?.let { add(it, BorderLayout.CENTER) }
            }
            refreshPlayingIconInList()
            revalidate()
            repaint()
        } else {
            JOptionPane.showMessageDialog(null, bundle.message("message.empty.folder"))
        }
    }

    private fun createMusicPanel(music: File): JPanel {
        return JPanel().apply {
            layout = BorderLayout()
            if (music === currentMusic) {
                add(JLabel().apply { icon = MusicIcons.playing }, BorderLayout.WEST)
            }
            add(JLabel(" " + music.name), BorderLayout.CENTER)
            if (music in currentLikeList) {
                add(JLabel().apply { icon = MusicIcons.like }, BorderLayout.EAST)
            } else if (music in currentDislikeList) {
                add(JLabel().apply { icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike }, BorderLayout.EAST)
            }
        }
    }

    private fun playMusic() {
        alonePlayTime = 0
        showCover()
        if (defaultSettingHeight == 0) defaultSettingHeight = settingPanel.preferredSize.height
        if (!isPlaying && selectedMusic != null) {
            try {
                when (currentMusic!!.extension.lowercase(Locale.getDefault())) {
                    "mp3" -> {
                        val fileStream = currentMusic?.let { FileInputStream(it) }
                        player = AdvancedPlayer(fileStream)
                        if (mp3PlayThread != null) if (mp3PlayThread!!.isAlive) mp3PlayThread!!.interrupt()
                        mp3PlayThread = Thread {
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
                        mp3PlayThread!!.start()
                        isPlaying = true
                        playButton.text = getExplainableMessage("button.stop.tooltip")
                        playButton.icon = MusicIcons.stop
                    }
                    "ogg" -> {
                        if (oggPlayThread != null) if (oggPlayThread!!.isAlive) oggPlayThread!!.interrupt()
                        oggPlayThread = Thread {
                            try {
                                oggPlayer.play(currentMusic!!.path)
                            } catch (e: OggPlayerException) {
                                println("Play ERROR: ${e.message}")
                            }
                        }
                        oggPlayer.removePlaybackListener()
                        oggPlayer.addPlaybackListener(
                            object: OggPlaybackListener {
                                override fun onPlaybackFinished(filePath: String) {
                                    stopMusic()
                                    playCase()
                                }
                                override fun onPlaybackStopped(filePath: String, dueToError: Boolean) {
                                }
                                override fun onPlaybackError(
                                    filePath: String,
                                    e: OggPlayerException
                                ) {
                                }
                            }
                        )
                        oggPlayThread!!.start()
                        isPlaying = true
                        playButton.text = getExplainableMessage("button.stop.tooltip")
                        playButton.icon = MusicIcons.stop
                    }
                    else -> {
                        val audioStream = currentMusic?.let { AudioSystem.getAudioInputStream(it) }
                        clip.open(audioStream)
                        clip.start()
                        clip.removeLineListener { it?.type == LineEvent.Type.STOP }
                        clip.addLineListener { event: LineEvent ->
                            if (event.type === LineEvent.Type.STOP && !objectivePause) {
                                stopMusic()
                                playCase()
                            }
                        }
                        isPlaying = true
                        playButton.text = getExplainableMessage("button.stop.tooltip")
                        playButton.icon = MusicIcons.stop
                    }
                }
                refreshPlayingIconInList()
                revalidate()
                repaint()
            } catch (_: Exception) {}
        } else {
            stopMusic()
        }
    }

    private fun stopMusic() {
        hideCover()
        if (player != null && mp3PlayThread != null) {
            player!!.close()
            if (mp3PlayThread!!.isAlive) mp3PlayThread!!.interrupt()
        }
        if (oggPlayThread != null) {
            oggPlayer.stop()
            if (oggPlayThread!!.isAlive) oggPlayThread!!.interrupt()
        }
        clip.close()
        alonePlayTime = 0
        isPlaying = false
        refreshPlayingIconInList()
        playButton.text = getExplainableMessage("button.play.tooltip")
        playButton.icon = MusicIcons.run
        revalidate()
        repaint()
    }

    fun JPanel.setHeight(height: Int) {
        preferredSize = Dimension(preferredSize.width, height)
    }

    private fun showCover() {
        coverPanel.edgeLength = size.width
        settingPanel.setHeight(0)
        revalidate()
        repaint()
    }

    private fun hideCover() {
        coverPanel.edgeLength = 0
        settingPanel.setHeight(defaultSettingHeight)
        revalidate()
        repaint()
    }

    private fun refreshAllButtonTooltips() {
        refreshLikeButtonVisuals()
        refreshPlayCaseButtonVisuals()
        playButton.text = if (isPlaying) getExplainableMessage("button.stop.tooltip") else getExplainableMessage("button.play.tooltip")
        nextButton.text = getExplainableMessage("button.next.tooltip")
        prevButton.text = getExplainableMessage("button.prev.tooltip")
        rescanButton.text = getExplainableMessage("button.rescan.tooltip")
    }

    private fun playCase() {
        when (state.playCase) {
            0 -> { // List Cycle
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
            1 -> { // List Reverse Cycle
                if (musicFiles.isNotEmpty()) {
                    var index = musicFiles.indexOf(currentMusic)
                    index--
                    if (index < 0) index = musicFiles.size - 1
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            2 -> { // Alone Cycle
                if (currentMusic != null) {
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            3 -> { // Alone Finite Cycle
                if (currentMusic != null && alonePlayTime < state.alonePlayTimes) {
                    refreshPlayingIconInList()
                    alonePlayTime++
                    playMusic()
                }
            }
            4 -> { // List Play
                if (musicFiles.isNotEmpty()) {
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) index = -1
                    index++
                    if (index >= musicFiles.size) return
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            5 -> { // List Reverse Play
                if (musicFiles.isNotEmpty()) {
                    var index = musicFiles.indexOf(currentMusic)
                    index--
                    if (index < 0) return
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    refreshPlayingIconInList()
                    playMusic()
                }
            }
            6 -> { // Random
                randomPlayMusic(false)
            }
            7 -> { // Random Finite
                randomPlayMusic(true)
            }
            8 -> { // Stop on Finish
                refreshPlayingIconInList()
            }
        }
    }

    fun Double.compare(value: Double): Boolean {
        val roundedThis = round(this * 10.0) / 10.0
        val roundedOther = round(value * 10.0) / 10.0
        return roundedThis == roundedOther
    }

    private fun randomPlayMusic(recordPlayedMusic: Boolean) {
        if (playedMusic.size == musicFiles.size) return
        if (musicFiles.size <= 1) {
            if (recordPlayedMusic) playMusic()
        } else if (state.likeWeight.compare(0.0) && state.dislikeWeight.compare(0.0)) {
            var index = floor(Math.random() * musicFiles.size).toInt()
            while ((state.antiAgainLevel == 2) && currentMusic === musicFiles[index]) {
                index = floor(Math.random() * musicFiles.size).toInt()
            }
            currentMusic = musicFiles[index]
        } else if (state.likeWeight.compare(0.0)) {
            val chooseDislike = if (state.dislikeWeight < 0) (Math.random() < (1 + state.dislikeWeight) * 0.1) else (Math.random() < state.dislikeWeight)
            currentMusic = weightChooseMusic(true, chooseDislike, true)
        } else if (state.dislikeWeight.compare(0.0)) {
            val chooseLike = if (state.likeWeight < 0) (Math.random() < (1 + state.likeWeight) * 0.1) else (Math.random() < state.likeWeight)
            currentMusic = weightChooseMusic(chooseLike, chooseDislike = true, withNormal = true)
        } else if (!state.likeWeight.compare(0.0) && !state.dislikeWeight.compare(0.0)) {
            val chooseLike = if (state.likeWeight < 0) (Math.random() < (1 + state.likeWeight) * 0.1) else (Math.random() < state.likeWeight)
            val chooseDislike = if (state.dislikeWeight < 0) (Math.random() < (1 + state.dislikeWeight) * 0.1) else (Math.random() < state.dislikeWeight)
            currentMusic = weightChooseMusic(chooseLike, chooseDislike, false)
        } else throw RandomPlayException("Unknown ERROR: Illegal value")
        if (currentMusic == null) throw RandomPlayException("Unknown ERROR: currentMusic is null")
        if (recordPlayedMusic && !playedMusic.contains(currentMusic!!)) playedMusic.add(currentMusic!!)
        selectedMusic = currentMusic
        list.selectedIndex = musicFiles.indexOf(currentMusic)
        refreshPlayingIconInList()
        playMusic()
    }

    private fun weightChooseMusic(chooseLike: Boolean, chooseDislike: Boolean, withNormal: Boolean): File {
        var tempList = ArrayList<File>()
        if (chooseLike && state.likeWeight != 1.0) tempList.addAll(currentLikeList)
        if (chooseDislike && state.dislikeWeight != 1.0) tempList.addAll(currentDislikeList)
        if (withNormal) tempList.addAll(currentNormalList)
        if (tempList.size > 1) {
            var index = floor(Math.random() * tempList.size).toInt()
            while ((state.antiAgainLevel == 2) && currentMusic === tempList[index]) {
                index = floor(Math.random() * tempList.size).toInt()
            }
            return tempList[index]
        } else if (tempList.size == 1) {
            return tempList[0]
        } else {
            var index = floor(Math.random() * musicFiles.size).toInt()
            while ((state.antiAgainLevel == 2) && currentMusic === musicFiles[index]) {
                index = floor(Math.random() * musicFiles.size).toInt()
            }
            return musicFiles[index]
        }
    }

    // TODO: Volume Control
    private fun setClipVolume(clip: Clip, volumeDB: Float) {
        if (clip.isOpen) {
            try {
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                    val min = gainControl.minimum
                    val max = gainControl.maximum
                    var actualVolumeDB = volumeDB
                    if (actualVolumeDB < min) actualVolumeDB = min
                    if (actualVolumeDB > max) actualVolumeDB = max
                    gainControl.value = actualVolumeDB
                } else if (clip.isControlSupported(FloatControl.Type.VOLUME)) {
                    val volumeControl = clip.getControl(FloatControl.Type.VOLUME) as FloatControl
                    val linearVolume = Math.pow(10.0, (volumeDB / 20.0)).toFloat()
                    val minLin = volumeControl.minimum
                    val maxLin = volumeControl.maximum
                    var actualLinearVolume = linearVolume
                    if (actualLinearVolume < minLin) actualLinearVolume = minLin
                    if (actualLinearVolume > maxLin) actualLinearVolume = maxLin
                    volumeControl.value = actualLinearVolume
                } else {
                    println("Clip: Volume control not supported.")
                }
            } catch (e: IllegalArgumentException) {
                println("Error setting clip volume: ${e.message}")
            }
        }
    }
}

