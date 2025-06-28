package com.xxyxxdmc.hoshisuki

import com.intellij.ide.HelpTooltip
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.xxyxxdmc.RandomPlayException
import com.xxyxxdmc.component.*
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
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import javax.swing.*
import kotlin.math.floor
import kotlin.math.round

@Suppress("NestedLambdaShadowedImplicitParameter")
class HoshisukiUI : JPanel() {
    // 语言文件初始化
    // 让我们说中文！
    // Let's speak in English!
    // 日本語話しましう！
    private val bundle = HoshisukiBundle
    // 防止某些人不知道插音频设备(比如作者，某次没插音频设备发现的)
    init {
        try {
            AudioSystem.getClip()
        } catch (_ : Exception) {
            JOptionPane.showMessageDialog(null,
                bundle.message("message.no.audio.device"),
                bundle.message("message.error.title"),
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
    // 配置文件初始化
    private val state = HoshisukiSettings.instance
    // 简单的获取详细信息的方法
    private fun getExplainableMessage(key: String): String {
        return bundle.message(if (state.detailTooltip) "$key.detail" else key)
    }
    // 按钮的初始化
    private val selectButton = JButton(bundle.message("button.choose.text")).apply { icon = MusicIcons.folder }
    private val playButton = IconTooltipActionButton(MusicIcons.run, getExplainableMessage("button.play.tooltip")) {}
    private val nextButton = IconTooltipActionButton(MusicIcons.playForward, getExplainableMessage("button.next.tooltip")) {}
    private val prevButton = IconTooltipActionButton(MusicIcons.playBack, getExplainableMessage("button.prev.tooltip")) {}
    private val rescanButton = IconTooltipActionButton(MusicIcons.rescan, getExplainableMessage("button.rescan.tooltip"), false, {}) {}
    private val settingButton = IconTooltipActionButton(MusicIcons.setting, getExplainableMessage("button.setting.tooltip"), true) {}
    private var playCase = IconTooltipActionButton(MusicIcons.listCycle, "") {}
    // 一些数值的初始化
    private var defaultSettingHeight: Int = 0
    private var alonePlayTime: Int = 0
    // 音乐文件列表的初始化
    private var musicFiles = ArrayList<File>()
    private var currentLikeList = ArrayList<File>()
    private var currentDislikeList = ArrayList<File>()
    private var currentNormalList = ArrayList<File>()
    private var playedMusic = ArrayList<File>()
    // 表的初始化
    private val musicFolderMap = mutableMapOf<String, List<File>>()
    private val musicFolderStateMap = mutableMapOf<String, Boolean>()
    private val musicFolderModelList = DefaultListModel<JPanel>()
    private var listPanel: ListPanel? = null
    // 播放器的初始化
    private var mp3Player: AdvancedPlayer? = null
    private val clip: Clip = AudioSystem.getClip()
    private val oggPlayer: OggPlayer = OggPlayer()
    // 播放状态的初始化
    private var isPlaying: Boolean = false
    private var switchMusic: Boolean = false
    private var objectivePause: Boolean = false
    // 播放线程的初始化
    private var mp3PlayThread: Thread? = null
    private var oggPlayThread: Thread? = null
    // 临时文件初始化
    private var selectedMusic: File? = null
    private var currentMusic: File? = null
    private var musicCoverTempFolder: File? = null
    private var tempCover: File? = null
    // 面板的初始化
    private val folderLabel = JLabel(bundle.message("folder.label.not.chosen"))
    private val coverPanel = CoverPanel(null, size.height, -1) {
        if (selectedMusic != null) {
            if (state.musicCoverMap.keys.contains(selectedMusic!!.absolutePath)) removeCover()
            else chooseCover()
        }
    }
    private var scrollPane: Component? = null
    private var settingPanel: JPanel = JPanel()

    // 类初始值设定项(本来不想用这么专业的名字的...)
    init {
        minimumSize = Dimension(150, 0)
        layout = BorderLayout()

        val controlPanel = JPanel().apply {
            add(settingButton)
            add(prevButton)
            add(playButton)
            add(nextButton)
            add(playCase)
        }
        val displayPanel = JPanel().apply {
            layout = BorderLayout()
            add(JLabel("  "+bundle.message("music.folder.label.prefix")+" "), BorderLayout.WEST)
            add(folderLabel, BorderLayout.CENTER)
            folderLabel.text = state.musicFolderList.size.toString() + " Folders"
            add(JPanel().apply {
                layout = BorderLayout()
                add(rescanButton, BorderLayout.WEST)
                add(selectButton, BorderLayout.CENTER)
            }, BorderLayout.EAST)
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
                        displayMusicFolderList()
                        revalidate()
                        repaint()
                    }
                }, BorderLayout.EAST)
            })
            //添加空缺
            add(Box.createVerticalStrut(7))
            //添加美化标题面板
            add(JPanel().apply {
                layout = BorderLayout()
                add(JLabel("  "+bundle.message("option.beauty.title.text")).apply {
                    HelpTooltip().setDescription(bundle.message("option.beauty.title.context")).installOn(this)
                })
                val options = arrayOf(
                    bundle.message("option.beauty.title.none"),
                    bundle.message("option.beauty.no.underscore"),
                    bundle.message("option.beauty.uppercase"),
                    bundle.message("option.beauty.pascal.case"),
                    bundle.message("option.beauty.special.case"),
                    bundle.message("option.beauty.title.case"))
                add(JPanel().apply {
                    layout = BorderLayout()
                    add(JBCheckBox("", state.beautifyTitleEnabled).apply {
                        addActionListener {
                            state.beautifyTitleEnabled = this.isSelected
                            if (state.musicFolderList.isNotEmpty() && state.beautifyTitle != 0) {
                                displayMusicFolderList()
                            }
                        }
                    }, BorderLayout.WEST)
                    add(JComboBox(options).apply {
                        selectedIndex = state.beautifyTitle
                        addItemListener { event ->
                            if (event.stateChange == ItemEvent.SELECTED) {
                                if (state.beautifyTitle != this.selectedIndex) {
                                    state.beautifyTitle = this.selectedIndex
                                    if (state.musicFolderList.isNotEmpty() && state.beautifyTitleEnabled) {
                                        displayMusicFolderList()
                                    }
                                }
                            }
                        }
                    }, BorderLayout.CENTER)
                }, BorderLayout.EAST)
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
                    addItemListener { event ->
                        if (event.stateChange == ItemEvent.SELECTED) {
                            state.antiAgainLevel = this.selectedIndex
                        }
                    }
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

        add(JPanel().apply {
            layout = BorderLayout()
            add(coverPanel, BorderLayout.CENTER)
            add(JSeparator(SwingConstants.HORIZONTAL), BorderLayout.SOUTH)
            add(displayPanel, BorderLayout.SOUTH)
        }, BorderLayout.NORTH)

        add(JPanel().apply { layout = BorderLayout()
            add(controlPanel, BorderLayout.SOUTH)
            add(settingPanel, BorderLayout.CENTER)
        }, BorderLayout.SOUTH)

        selectButton.addActionListener { chooseFolder() }

        folderLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {}
            override fun mouseEntered(e: MouseEvent?) {
                if (state.musicFolderList.isNotEmpty()) {
                    folderLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    super.mouseEntered(e)
                }
            }
            override fun mouseExited(e: MouseEvent?) {
                if (state.musicFolderList.isNotEmpty()) {
                    folderLabel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                }
            }
        })

        // Action 初始化
        playButton.action = Runnable {
            if (state.musicFolderList.isNotEmpty()) {
                playButton.isEnabled = false
                objectivePause = true
                playedMusic.clear()
                if (isPlaying && selectedMusic?.absolutePath != currentMusic?.absolutePath && selectedMusic?.isFile == true) {
                    currentMusic = selectedMusic
                    stopMusic(true)
                } else if (isPlaying && (selectedMusic == null || selectedMusic?.absolutePath == currentMusic?.absolutePath)) stopMusic()
                else if (isPlaying && selectedMusic?.isDirectory == true) {
                    currentMusic = musicFolderMap[selectedMusic!!.absolutePath]!![0]
                    stopMusic(true)
                } else {
                    currentMusic = if (selectedMusic?.isFile == true) {
                        selectedMusic
                    } else if (selectedMusic?.isDirectory == true) {
                        musicFolderMap[selectedMusic!!.absolutePath]!![0]
                    } else if (musicFiles.isNotEmpty()) {
                        musicFiles[0]
                    } else null
                    stopMusic(true)
                }
                revalidate()
                repaint()
                objectivePause = false
                java.util.Timer().schedule(object: TimerTask() {
                    override fun run() {
                        playButton.isEnabled = true
                    }
                }, 1000)
            }
        }
        rescanButton.action = Runnable {
            if (state.musicFolderList.isNotEmpty()) {
                state.musicFolderList.forEach {
                    musicFolderStateMap[it] = true
                }
                displayMusicFolderList()
            }
        }
        rescanButton.action2 = Runnable {
            if (state.musicFolderList.isNotEmpty()) {
                state.musicFolderList.forEach {
                    musicFolderStateMap[it] = false
                }
                displayMusicFolderList()
            }
        }
        prevButton.action = Runnable {
            if (state.musicFolderList.isNotEmpty() && isPlaying && musicFiles.size > 1) {
                playedMusic.clear()
                switchMusic = true
                objectivePause = true
                if (prevButton.icon == MusicIcons.playBack) {
                    var index = musicFiles.indexOf(currentMusic)
                    index--
                    if (index < 0) index = musicFiles.size - 1
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    stopMusic(true)
                } else {
                    currentMusic = musicFiles.last()
                    selectedMusic = musicFiles.last()
                    stopMusic(true)
                }
                objectivePause = false
                switchMusic = false
                java.util.Timer().schedule(object: TimerTask() {
                    override fun run() {
                        prevButton.isEnabled = true
                    }
                }, 1000)
            }
        }
        nextButton.action = Runnable {
            if (state.musicFolderList.isNotEmpty() && isPlaying && musicFiles.size > 1) {
                playedMusic.clear()
                switchMusic = true
                objectivePause = true
                if (nextButton.icon == MusicIcons.playForward) {
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) index = -1
                    index++
                    if (index >= musicFiles.size) index = 0
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    stopMusic(true)
                } else {
                    currentMusic = musicFiles.first()
                    selectedMusic = musicFiles.first()
                    stopMusic(true)
                }
                objectivePause = false
                switchMusic = false
                java.util.Timer().schedule(object: TimerTask() {
                    override fun run() {
                        nextButton.isEnabled = true
                    }
                }, 1000)
            }
        }
        playCase.action = Runnable {
            if (state.playCase + 1 > 8) state.playCase = 0
            else state.playCase++
            playedMusic.clear()
            refreshPlayCaseButtonVisuals()
        }
        settingButton.action = Runnable {
            if (!settingButton.isLatched) {
                hideSetting()
            } else {
                showSetting()
            }
        }

        if (state.musicFolderList.isNotEmpty()) {
            if (state.musicFolder != null)
            if (!state.musicFolderList.contains(state.musicFolder)) state.musicFolderList.add(state.musicFolder!!)
            displayMusicFolderList()
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
        coverPanel.contextMenu = JPopupMenu().apply {
            val addPrevCover = JMenuItem(bundle.message("menu.add.prev.cover.text")).apply {
                addActionListener {
                    if (selectedMusic != null) {
                        if (state.musicCoverMap.keys.contains(selectedMusic!!.absolutePath)) removeCover()
                        if (tempCover == null) {
                            chooseCover()
                        } else {
                            state.musicCoverMap[selectedMusic!!.absolutePath] = tempCover!!.absolutePath
                            if (coverPanel.size.height != 0 && currentMusic === selectedMusic) {
                                coverPanel.cover = ImageIcon(tempCover!!.absolutePath)
                                repaint()
                            }
                        }
                    }
                }
            }
            val removeCover = JMenuItem(bundle.message("menu.remove.cover.text")).apply {
                if (selectedMusic != null) {
                    if (state.musicCoverMap.keys.contains(selectedMusic!!.absolutePath)) removeCover()
                }
            }
            add(addPrevCover)
            add(removeCover)
        }

        refreshPlayCaseButtonVisuals()

        if (defaultSettingHeight <= 0) defaultSettingHeight = (if (settingPanel.size.height <= 0) 268 else settingPanel.size.height)

        settingButton.isLatched = true
        settingButton.isEnabled = false
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

    private fun chooseFolder() {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY; isMultiSelectionEnabled = false }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            stopMusic()
            currentMusic = null
            selectedMusic = null
            state.musicFolderList.apply { if (!this.contains(chooser.selectedFile.absolutePath)) add(chooser.selectedFile.absolutePath) }
            displayMusicFolderList()
        }
    }

    private fun chooseCover() {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = false
            isAcceptAllFileFilterUsed = false
            fileFilter = PictureFileFilter()
            if (musicCoverTempFolder != null) currentDirectory = musicCoverTempFolder!!
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            state.musicCoverMap[selectedMusic!!.absolutePath] = chooser.selectedFile.absolutePath
            musicCoverTempFolder = chooser.currentDirectory
            if (tempCover?.absolutePath != chooser.selectedFile.absolutePath) tempCover = chooser.selectedFile
            if (coverPanel.size.height != 0 && currentMusic?.absolutePath == selectedMusic!!.absolutePath) {
                coverPanel.cover = ImageIcon(chooser.selectedFile.absolutePath)
                repaint()
            }
        }
    }

    private fun removeCover() {
        state.musicCoverMap.remove(selectedMusic!!.absolutePath)
        if (coverPanel.size.height != 0 && currentMusic === selectedMusic) {
            coverPanel.cover = null
            repaint()
        }
    }

    private fun displayMusicFolderList() {
        val noMusicFolder = ArrayList<File>()
        val noSupportMusicFolder = ArrayList<File>()
        folderLabel.text = state.musicFolderList.size.toString() + " Folder(s)"
        folderLabel.apply {
            val sb = StringBuilder()
            state.musicFolderList.forEach { sb.append(it).append("<br>") }
            HelpTooltip().setDescription("<html>$sb</html>").installOn(this)
        }
        musicFolderMap.clear()
        musicFolderModelList.clear()
        musicFiles.clear()
        currentDislikeList.clear()
        currentLikeList.clear()
        currentNormalList.clear()
        for (folderPath in state.musicFolderList) {
            val folder = File(folderPath)
            val musicList = ArrayList<File>()
            val musicListModel = ArrayList<JPanel>()
            if (folder.exists() && folder.isDirectory && folder.listFiles().isNotEmpty()) {
                folder.listFiles().forEach {
                    if (it.extension.lowercase(Locale.getDefault()) in listOf("mp3", "wav", "aif", "aiff", "au", "ogg")) {
                        musicList.add(it)
                        when (it.absolutePath) {
                            in state.likeList -> currentLikeList.add(it)
                            in state.dislikeList -> currentDislikeList.add(it)
                            else -> currentNormalList.add(it)
                        }
                        val processedName = if (state.beautifyTitleEnabled) when (state.beautifyTitle) {
                            1 -> it.nameWithoutExtension.replace("_", " ")
                            2 -> it.nameWithoutExtension.let { it.first().uppercase() + it.substring(1) }
                            3 -> it.nameWithoutExtension.let { it.split("_").joinToString("") { it.first().uppercase() + it.substring(1) }}
                            4 -> it.nameWithoutExtension.let { it.first().uppercase() + it.substring(1) }.replace("_", " ")
                            5 -> it.nameWithoutExtension.let { it.split("_").joinToString(" ") { it.first().uppercase() + it.substring(1) }}
                            else -> it.nameWithoutExtension
                        } else it.name
                        val likeInfo = when {
                            (it in currentLikeList) -> 1
                            (it in currentDislikeList && state.sensitiveIcon) -> 3
                            (it in currentDislikeList && !state.sensitiveIcon) -> 2
                            else -> 0
                        }
                        val tooltip = when {
                            currentLikeList.contains(it) -> getExplainableMessage("button.like.tooltip")
                            currentDislikeList.contains(it) -> getExplainableMessage("button.dislike.tooltip")
                            else -> getExplainableMessage("button.un.dislike.tooltip")
                        }
                        musicListModel.add(MusicPanel(it.absolutePath, processedName, likeInfo) {}.apply {
                            isPlaying = this@HoshisukiUI.isPlaying && it.absolutePath == currentMusic?.absolutePath
                            this.likeButton.text = tooltip
                            action = Runnable {
                                //if (!this.isVisible) this.isVisible = true
                                when (it) {
                                    in currentLikeList -> {
                                        currentLikeList.remove(it)
                                        state.likeList.remove(it.absolutePath)
                                        currentDislikeList.add(it)
                                        state.dislikeList.add(it.absolutePath)
                                        this.likeButton.icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
                                        this.likeButton.text = getExplainableMessage("button.dislike.tooltip")
                                    }
                                    in currentDislikeList -> {
                                        currentDislikeList.remove(it)
                                        state.dislikeList.remove(it.absolutePath)
                                        currentNormalList.add(it)
                                        this.likeButton.icon = MusicIcons.unDislike
                                        this.likeButton.text = getExplainableMessage("button.un.dislike.tooltip")
                                    }
                                    else -> {
                                        currentLikeList.add(it)
                                        state.likeList.add(it.absolutePath)
                                        currentNormalList.remove(it)
                                        this.likeButton.icon = MusicIcons.like
                                        this.likeButton.text = getExplainableMessage("button.like.tooltip")
                                    }
                                }
                            }
                        })
                    }
                }
                if (musicList.isEmpty()) {
                    noSupportMusicFolder.add(folder)
                    continue
                }
            } else {
                noMusicFolder.add(folder)
                continue
            }
            musicFolderMap[folderPath] = musicList
            musicFolderModelList.addElement(
                FolderPanel(
                (isPlaying && musicFolderMap[folderPath]!!.contains(currentMusic)),
                folderPath,
                bundle.message("button.open.folder.tooltip"),
                    musicFolderStateMap[folderPath] == true || musicFolderStateMap[folderPath] == null
                    ,{}) {
                    state.musicFolderList.remove(folderPath)
                    displayMusicFolderList()
                }.apply {
                action = Runnable {
                    musicFolderStateMap[folderPath] = !this.state
                    displayMusicFolderList()
                }
            })
            musicFiles.addAll(musicList)
            if ((musicFolderStateMap[folderPath] == null) || musicFolderStateMap[folderPath]!!) musicListModel.forEach {
                musicFolderModelList.addElement(it)
            }
        }
        playButton.isEnabled = true
        scrollPane?.let { remove(it) }
        listPanel = ListPanel(musicFolderModelList).apply {
            removeListeners()
            addListener { it ->
                if (it.music != null && it.music.isFile) {
                    selectedMusic = it.music
                    this@apply.setSelectedItem(it.music)
                }
                if (isPlaying) {
                    if (selectedMusic?.absolutePath != currentMusic!!.absolutePath) {
                        playButton.icon = MusicIcons.resume
                        playButton.text = getExplainableMessage("button.stop.and.play.tooltip")
                    } else {
                        playButton.icon = MusicIcons.stop
                        playButton.text = getExplainableMessage("button.stop.tooltip")
                    }
                } else {
                    playButton.icon = MusicIcons.run
                    playButton.text = getExplainableMessage("button.play.tooltip")
                }
            }
        }
        scrollPane = JBScrollPane(listPanel).apply {
            preferredSize = Dimension(200, 150)
        }
        if (scrollPane != null) {
            scrollPane?.let { add(scrollPane!!, BorderLayout.CENTER) }
            revalidate()
            repaint()
        }
        refreshAllButtonTooltips()
        val errorFrame: JFrame = JFrame(bundle.message("message.error.title")).apply frame@{
            layout = BorderLayout()
            size = Dimension(400, 160)
            setLocationRelativeTo(null)
            add(JLabel("  " + bundle.message("message.no.or.support.music")), BorderLayout.NORTH)
            val errorFolderList = DefaultListModel<JPanel>()
            var listPanel = ListPanel(errorFolderList)
            noMusicFolder.forEach { each ->
                errorFolderList.addElement(JPanel().apply {
                    layout = BorderLayout()
                    add(IconTooltipActionButton(
                        MusicIcons.noMusic,
                        bundle.message("button.open.folder.no.music.tooltip"),
                        false,
                        {
                            try {
                                Desktop.getDesktop().open(each)
                            } catch (_: Exception) {}
                        }
                    ) {
                        listPanel.removePanel(this)
                        this@HoshisukiUI.state.musicFolderList.remove(each.absolutePath)
                        this@frame.repaint()
                        if (listPanel.isEmpty) this@frame.dispose()
                    }, BorderLayout.WEST)
                    add(JLabel(each.absolutePath), BorderLayout.CENTER)
                })
            }
            noSupportMusicFolder.forEach { each ->
                errorFolderList.addElement(JPanel().apply {
                    layout = BorderLayout()
                    add(IconTooltipActionButton(
                        MusicIcons.noSupportMusic,
                        bundle.message("button.open.folder.no.support.music.tooltip"),
                        false,
                        {
                            try {
                                Desktop.getDesktop().open(each)
                            } catch (_: Exception) {}
                        }
                    ) {
                        listPanel.removePanel(this)
                        this@HoshisukiUI.state.musicFolderList.remove(each.absolutePath)
                        this@frame.repaint()
                        if (listPanel.isEmpty) this@frame.dispose()
                    }, BorderLayout.WEST)
                    add(JLabel(each.absolutePath), BorderLayout.CENTER)
                })
            }
            listPanel = ListPanel(errorFolderList)
            add(JPanel().apply {
                add(JButton(bundle.message("button.clear.all.text")).apply {
                    addActionListener {
                        if (this.text == bundle.message("button.confirm.text")) {
                            this@frame.dispose()
                            noMusicFolder.forEach { this@HoshisukiUI.state.musicFolderList.remove(it.absolutePath) }
                            noSupportMusicFolder.forEach { this@HoshisukiUI.state.musicFolderList.remove(it.absolutePath) }
                        } else {
                            this.text = bundle.message("button.confirm.text")
                        }
                    }
                })
                add(JButton(bundle.message("button.ignore.text")).apply {
                    addActionListener {
                        this@frame.dispose()
                    }
                })
            }, BorderLayout.SOUTH)
            add(JBScrollPane(listPanel).apply { preferredSize = Dimension(200, 150) }, BorderLayout.CENTER)
            isVisible = false
        }
        if (noMusicFolder.size + noSupportMusicFolder.size > 0) errorFrame.isVisible = true
    }

    private fun playMusic() {
        alonePlayTime = 0
        if (!isPlaying && currentMusic != null) {
            refreshCover()
            listPanel!!.setSelectedItem(currentMusic)
            try {
                when (currentMusic!!.extension.lowercase(Locale.getDefault())) {
                    "mp3" -> {
                        if (mp3PlayThread?.isAlive == true) {
                            mp3Player?.close()
                            mp3PlayThread?.interrupt()
                            mp3PlayThread = null
                        }
                        mp3Player = AdvancedPlayer(FileInputStream(currentMusic!!))

                        val newMp3PlayThread = Thread {
                            try {
                                mp3Player?.play()
                            } catch (e: JavaLayerException) {
                                if (!(e.message?.contains("Interrupted", ignoreCase = true) == true || e.cause is InterruptedException)) {
                                    System.err.println("MP3Player Playback ERROR: ${e.message}")
                                    e.printStackTrace()
                                }
                            } catch (e: Exception) {
                                System.err.println("MP3Player Unexpected Playback ERROR: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                        mp3PlayThread = newMp3PlayThread
                        mp3Player?.playBackListener = object : PlaybackListener() {
                            override fun playbackFinished(evt: PlaybackEvent?) {
                                if (evt?.source == mp3Player && isPlaying) {
                                    currentMusic = playCase()
                                    stopMusic(true)
                                }
                            }
                        }
                        mp3PlayThread?.start()
                    }
                    "ogg" -> {
                        if (oggPlayThread?.isAlive == true) {
                            oggPlayer.stop()
                            oggPlayThread?.interrupt()
                            // oggPlayThread?.join(100) // Avoid join if called from ogg thread itself
                            oggPlayThread = null
                        }
                        oggPlayThread = Thread {
                            try {
                                oggPlayer.play(currentMusic!!.path)
                            } catch (e: OggPlayerException) {
                                System.err.println("OggPlayer ERROR: ${e.message}")
                            } catch (e: Exception) {
                                System.err.println("OggPlayer Unexpected ERROR: ${e.message}")
                                e.printStackTrace()
                            }
                        }
                        oggPlayer.removePlaybackListener()
                        oggPlayer.addPlaybackListener(
                            object : OggPlaybackListener {
                                override fun onPlaybackFinished(filePath: String) {
                                    currentMusic = playCase()
                                    stopMusic(true)
                                }
                                override fun onPlaybackStopped(filePath: String, dueToError: Boolean) {}
                                override fun onPlaybackError(filePath: String, e: OggPlayerException) {
                                    JOptionPane.showMessageDialog(this@HoshisukiUI, "Ogg playback error: ${e.message}", "Playback Error", JOptionPane.ERROR_MESSAGE)
                                }
                            }
                        )
                        oggPlayThread!!.start()
                    }
                    else -> { // wav, aif, au
                        if (clip.isOpen) {
                            clip.stop()
                            clip.flush()
                            clip.close()
                        }
                        val audioStream = AudioSystem.getAudioInputStream(currentMusic)
                        clip.open(audioStream)
                        clip.start()
                        clip.removeLineListener { it?.type == LineEvent.Type.STOP }
                        clip.addLineListener { event: LineEvent ->
                            if (event.type === LineEvent.Type.STOP && !objectivePause) {
                                if (event.line == clip && isPlaying) {
                                    currentMusic = playCase()
                                    stopMusic(true)
                                }
                            }
                        }
                    }
                }
                isPlaying = true
                playButton.text = getExplainableMessage("button.stop.tooltip")
                playButton.icon = MusicIcons.stop
                displayMusicFolderList()
                revalidate()
                repaint()
            } catch (fnf: java.io.FileNotFoundException) {
                System.err.println("Play ERROR: File not found - ${currentMusic?.absolutePath} - ${fnf.message}")
                fnf.printStackTrace()
                stopMusic()
            } catch (e: Exception) {
                System.err.println("Play ERROR during setup or start: ${e.message}")
                e.printStackTrace()
                stopMusic(true)
            }
        } else if (isPlaying) {
            stopMusic()
        }
    }

    private fun stopMusic() {
        stopMusic(false)
    }

    private fun stopMusic(play: Boolean){
        val wasPlaying = isPlaying
        isPlaying = false

        val currentMp3Thread = mp3PlayThread
        if (currentMp3Thread != null && currentMp3Thread.isAlive) {
            mp3Player?.close()
            if (Thread.currentThread() != currentMp3Thread) {
                currentMp3Thread.interrupt()
                try {
                    currentMp3Thread.join(100)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    System.err.println("Interrupted while joining MP3 thread: ${e.message}")
                }
            }
        }
        mp3Player = null
        mp3PlayThread = null

        val currentOggThread = oggPlayThread
        if (currentOggThread != null && currentOggThread.isAlive) {
            oggPlayer.stop()
            if (Thread.currentThread() != currentOggThread) {
                currentOggThread.interrupt()
                try {
                    currentOggThread.join(100)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    System.err.println("Interrupted while joining OGG thread: ${e.message}")
                }
            }
        }
        oggPlayThread = null

        if (clip.isOpen) {
            clip.stop()
            clip.flush()
            clip.close()
        }

        if (wasPlaying) {
            alonePlayTime = 0
        }

        if (!play) {
            displayMusicFolderList()
            playButton.text = getExplainableMessage("button.play.tooltip")
            playButton.icon = MusicIcons.run
            playButton.isEnabled = musicFiles.isNotEmpty()
        } else playMusic()

        revalidate()
        repaint()
    }

    fun JPanel.setHeight(height: Int) {
        preferredSize = Dimension(preferredSize.width, height)
    }

    private fun refreshCover() {
        val currentSelectedPath = currentMusic?.absolutePath
        if (currentSelectedPath != null && state.musicCoverMap.containsKey(currentSelectedPath)) {
            coverPanel.cover = ImageIcon(state.musicCoverMap[currentSelectedPath])
        } else {
            coverPanel.cover = null
        }
        coverPanel.isVisible = true
        coverPanel.edgeLength = size.width
        settingPanel.isVisible = false
        settingButton.isEnabled = true
        settingButton.isLatched = false
        revalidate()
        repaint()
    }

    private fun showSetting() {
        settingPanel.isVisible = true
        revalidate()
        repaint()
    }

    private fun hideSetting() {
        settingPanel.isVisible = false
        revalidate()
        repaint()
    }

    private fun refreshAllButtonTooltips() {
        refreshPlayCaseButtonVisuals()
        playButton.text = if (isPlaying && selectedMusic?.absolutePath != currentMusic!!.absolutePath) getExplainableMessage("button.stop.and.play.tooltip")
        else if (isPlaying) getExplainableMessage("button.stop.tooltip")
        else getExplainableMessage("button.play.tooltip")
        nextButton.text = getExplainableMessage("button.next.tooltip")
        prevButton.text = getExplainableMessage("button.prev.tooltip")
        rescanButton.text = getExplainableMessage("button.rescan.tooltip")
    }

    private fun playCase(): File? {
        when (state.playCase) {
            0 -> { // List Cycle
                if (musicFiles.isNotEmpty()) {
                    switchMusic = true
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) index = -1
                    index++
                    if (index >= musicFiles.size) index = 0
                    return musicFiles[index]
                }
            }
            1 -> { // List Reverse Cycle
                if (musicFiles.isNotEmpty()) {
                    switchMusic = true
                    var index = musicFiles.indexOf(currentMusic)
                    index--
                    if (index < 0) index = musicFiles.size - 1
                    return musicFiles[index]
                }
            }
            2 -> { // Alone Cycle
                return if (currentMusic != null) {
                    currentMusic
                } else null
            }
            3 -> { // Alone Finite Cycle
                if (currentMusic != null && alonePlayTime < state.alonePlayTimes) {
                    alonePlayTime++
                    return currentMusic
                } else return null
            }
            4 -> { // List Play
                if (musicFiles.isNotEmpty()) {
                    switchMusic = true
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) index = -1
                    index++
                    if (index >= musicFiles.size) {
                        switchMusic = false
                        return null
                    }
                    return musicFiles[index]
                }
            }
            5 -> { // List Reverse Play
                if (musicFiles.isNotEmpty()) {
                    switchMusic = true
                    var index = musicFiles.indexOf(currentMusic)
                    index--
                    if (index < 0) {
                        switchMusic = false
                        return null
                    }
                    return musicFiles[index]
                }
            }
            6 -> { // Random
                return randomPlayMusic(false)
            }
            7 -> { // Random Finite
                return randomPlayMusic(true)
            }
            8 -> { // Stop on Finish
            }
        }
        return null
    }

    fun Double.compare(value: Double): Boolean {
        val roundedThis = round(this * 10.0) / 10.0
        val roundedOther = round(value * 10.0) / 10.0
        return roundedThis == roundedOther
    }

    private fun randomPlayMusic(recordPlayedMusic: Boolean): File? {
        if (playedMusic.size == musicFiles.size) return null
        switchMusic = true
        var chooseMusic: File? = null
        if (musicFiles.size <= 1) {
            if (recordPlayedMusic) {
                switchMusic = false
                return null
            }
        } else if (state.likeWeight.compare(0.0) && state.dislikeWeight.compare(0.0)) {
            var index = floor(Math.random() * musicFiles.size).toInt()
            while ((state.antiAgainLevel == 2) && currentMusic === musicFiles[index]) {
                index = floor(Math.random() * musicFiles.size).toInt()
            }
            chooseMusic = musicFiles[index]
        } else if (state.likeWeight.compare(0.0)) {
            val chooseDislike = if (state.dislikeWeight < 0) (Math.random() < (1 + state.dislikeWeight) * 0.1) else (Math.random() < state.dislikeWeight)
            chooseMusic = weightChooseMusic(true, chooseDislike, true)
        } else if (state.dislikeWeight.compare(0.0)) {
            val chooseLike = if (state.likeWeight < 0) (Math.random() < (1 + state.likeWeight) * 0.1) else (Math.random() < state.likeWeight)
            chooseMusic = weightChooseMusic(chooseLike, chooseDislike = true, withNormal = true)
        } else if (!state.likeWeight.compare(0.0) && !state.dislikeWeight.compare(0.0)) {
            val chooseLike = if (state.likeWeight < 0) (Math.random() < (1 + state.likeWeight) * 0.1) else (Math.random() < state.likeWeight)
            val chooseDislike = if (state.dislikeWeight < 0) (Math.random() < (1 + state.dislikeWeight) * 0.1) else (Math.random() < state.dislikeWeight)
            chooseMusic = weightChooseMusic(chooseLike, chooseDislike, false)
        } else throw RandomPlayException("Unknown ERROR: Illegal value")
        if (chooseMusic == null) {
            switchMusic = false
            throw RandomPlayException("Unknown ERROR: currentMusic is null")
        }
        if (recordPlayedMusic && !playedMusic.contains(chooseMusic)) playedMusic.add(chooseMusic)
        return chooseMusic
    }

    private fun weightChooseMusic(chooseLike: Boolean, chooseDislike: Boolean, withNormal: Boolean): File {
        val tempList = ArrayList<File>()
        if (chooseLike && state.likeWeight > -1.0) tempList.addAll(currentLikeList)
        if (chooseDislike && state.dislikeWeight > -1.0) tempList.addAll(currentDislikeList)
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
}

