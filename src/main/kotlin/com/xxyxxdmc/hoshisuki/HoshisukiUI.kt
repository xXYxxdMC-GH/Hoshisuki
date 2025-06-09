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
import kotlinx.coroutines.Runnable
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
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

final class HoshisukiUI : JPanel() {
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
                bundle.message("message.no.audio.device.title"),
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
    private val likeButton = IconTooltipActionButton(MusicIcons.like, getExplainableMessage("button.like.tooltip")) {}
    private val playButton = IconTooltipActionButton(MusicIcons.run, getExplainableMessage("button.play.tooltip")) {}
    private val nextButton = IconTooltipActionButton(MusicIcons.playForward, getExplainableMessage("button.next.tooltip")) {}
    private val prevButton = IconTooltipActionButton(MusicIcons.playBack, getExplainableMessage("button.prev.tooltip")) {}
    private val rescanButton = IconTooltipActionButton(MusicIcons.rescan, getExplainableMessage("button.rescan.tooltip")) {}
    private val settingButton = IconTooltipActionButton(MusicIcons.setting, getExplainableMessage("button.setting.tooltip"), true) {}
    private val coverButton = IconTooltipActionButton(MusicIcons.addCover, getExplainableMessage("button.add.cover.tooltip"), false, {}) {}
    private var playCase = IconTooltipActionButton(MusicIcons.listCycle, "") {}
    // 面板的初始化
    private val folderLabel = JLabel(bundle.message("folder.label.not.chosen"))
    private val coverPanel = CoverPanel(null, -1, -1)
    private var scrollPane: Component? = null
    private var settingPanel: JPanel = JPanel()
    // 一些数值的初始化
    private var defaultSettingHeight: Int = 0
    private var alonePlayTime: Int = 0
    // 音乐文件列表的初始化
    private var musicFiles = ArrayList<File>()
    private var currentLikeList = ArrayList<File>()
    private var currentDislikeList = ArrayList<File>()
    private var currentNormalList = ArrayList<File>()
    private var playedMusic = ArrayList<File>()
    // 列表的初始化
    private val listModel = DefaultListModel<File>()
    private val listModelPanel = DefaultListModel<JPanel>()
    private val list = JBList(listModelPanel)
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

    // 类初始值设定项(本来不想用这么专业的名字的...)
    init {
        minimumSize = Dimension(150, 0)
        layout = BorderLayout()

        val controlPanel = JPanel().apply {
            add(settingButton)
            add(likeButton)
            add(prevButton)
            add(playButton)
            add(nextButton)
            add(playCase)
            add(coverButton)
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
                            if (state.musicFolder != null && state.beautifyTitle != 0) {
                                displayMusicList(File(state.musicFolder!!))
                            }
                        }
                    }, BorderLayout.WEST)
                    add(JComboBox(options).apply {
                        selectedIndex = state.beautifyTitle
                        addItemListener { event ->
                            if (event.stateChange == java.awt.event.ItemEvent.SELECTED) {
                                if (state.beautifyTitle != this.selectedIndex) {
                                    state.beautifyTitle = this.selectedIndex
                                    if (state.musicFolder != null && state.beautifyTitleEnabled) {
                                        displayMusicList(File(state.musicFolder!!))
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
                        if (event.stateChange == java.awt.event.ItemEvent.SELECTED) {
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
            add(controlPanel, BorderLayout.NORTH)
            add(settingPanel, BorderLayout.CENTER)
        }, BorderLayout.SOUTH)

        selectButton.addActionListener { chooseFolder() }

        folderLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (state.musicFolder != null) {
                    Desktop.getDesktop().open(File(state.musicFolder!!))
                }
            }
            override fun mouseEntered(e: MouseEvent?) {
                folderLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                super.mouseEntered(e)
            }
            override fun mouseExited(e: MouseEvent?) {
                folderLabel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            }
        })

        // Action 初始化
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
                playButton.isEnabled = false
                objectivePause = true
                currentMusic = if (selectedMusic != null) {
                    selectedMusic
                } else if (musicFiles.isNotEmpty()) {
                    list.selectedIndex = 0
                    musicFiles[0]
                } else null

                playedMusic.clear()
                refreshLikeButtonVisuals()
                refreshCoverButtonVisuals()
                if (currentMusic != null) {
                    playMusic()
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
            if (state.musicFolder != null) {
                displayMusicList(File(state.musicFolder!!))
            }
        }
        prevButton.action = Runnable {
            if (state.musicFolder != null && isPlaying && musicFiles.size > 1) {
                playedMusic.clear()
                switchMusic = true
                objectivePause = true
                if (prevButton.icon == MusicIcons.playBack) {
                    var index = musicFiles.indexOf(currentMusic)
                    index--
                    if (index < 0) index = musicFiles.size - 1
                    stopMusic()
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    playMusic()
                } else {
                    stopMusic()
                    currentMusic = musicFiles.last()
                    selectedMusic = musicFiles.last()
                    list.selectedIndex = musicFiles.size - 1
                    playMusic()
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
            if (state.musicFolder != null && isPlaying && musicFiles.size > 1) {
                playedMusic.clear()
                switchMusic = true
                objectivePause = true
                if (nextButton.icon == MusicIcons.playForward) {
                    var index = musicFiles.indexOf(currentMusic)
                    if (index == -1) index = -1
                    index++
                    if (index >= musicFiles.size) index = 0
                    stopMusic()
                    currentMusic = musicFiles[index]
                    selectedMusic = musicFiles[index]
                    list.selectedIndex = index
                    playMusic()
                } else {
                    stopMusic()
                    currentMusic = musicFiles.first()
                    selectedMusic = musicFiles.first()
                    list.selectedIndex = 0
                    playMusic()
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
        coverButton.action = Runnable {
            if (state.musicFolder != null && selectedMusic != null) {
                if (state.musicCoverMap.keys.contains(selectedMusic!!.absolutePath)) removeCover()
                else chooseCover()
                refreshCoverButtonVisuals()
            }
        }
        coverButton.action2 = Runnable {
            if (state.musicFolder != null && selectedMusic != null) {
                if (state.musicCoverMap.keys.contains(selectedMusic!!.absolutePath)) removeCover()
                if (tempCover == null) {
                    chooseCover()
                    refreshCoverButtonVisuals()
                } else {
                    state.musicCoverMap[selectedMusic!!.absolutePath] = tempCover!!.absolutePath
                    if (coverPanel.size.height != 0 && currentMusic === selectedMusic) {
                        coverPanel.cover = ImageIcon(tempCover!!.absolutePath)
                        repaint()
                    }
                    refreshCoverButtonVisuals()
                }
            }
        }

        if (state.musicFolder!=null) {
            displayMusicList(File(state.musicFolder!!))
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

/*      addKeyListener( object: KeyListener {
            override fun keyTyped(e: KeyEvent?) {
            }
            override fun keyPressed(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_SHIFT && prevButton.icon == MusicIcons.playBack) {
                    prevButton.icon = MusicIcons.playFirst
                    nextButton.icon = MusicIcons.playLast
                    revalidate()
                    repaint()
                }
            }
            override fun keyReleased(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_SHIFT && prevButton.icon == MusicIcons.playFirst) {
                    prevButton.icon = MusicIcons.playBack
                    nextButton.icon = MusicIcons.playForward
                    revalidate()
                    repaint()
                }
            }
       }) */

        refreshPlayCaseButtonVisuals()

        if (defaultSettingHeight <= 0) (if (settingPanel.size.height <= 0) defaultSettingHeight = 268 else defaultSettingHeight = settingPanel.size.height)

        settingButton.isLatched = true
        settingButton.isEnabled = false

        if (musicFiles.isEmpty()) {
            playButton.isEnabled = false
            prevButton.isEnabled = false
            nextButton.isEnabled = false
            likeButton.isEnabled = false
            coverButton.isEnabled = false
        }
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

    private fun refreshCoverButtonVisuals() {
        if (selectedMusic != null) {
            if (state.musicCoverMap.containsKey(selectedMusic!!.absolutePath)) {
                coverButton.text = getExplainableMessage("button.remove.cover.tooltip")
                coverButton.icon = MusicIcons.removeCover
            } else {
                coverButton.text = getExplainableMessage("button.add.cover.tooltip")
                coverButton.icon = MusicIcons.addCover
            }
        } else {
            coverButton.text = getExplainableMessage("button.add.cover.tooltip")
            coverButton.icon = MusicIcons.addCover
        }
    }

    private fun refreshPlayingIconInList() {
        refreshLikeButtonVisuals()
        refreshCoverButtonVisuals()
        if (listModelPanel.isEmpty || listModel.isEmpty || listModelPanel.size() != listModel.size()) {
            return
        }

        var listNeedsRepaint = false

        for (i in 0 until listModelPanel.size()) {
            val panel = listModelPanel.getElementAt(i) ?: continue
            val fileForPanel = listModel.getElementAt(i) ?: continue

            if (state.optimizePanel) {
                val playingLabel = panel.components.find { it.name == "playingIndicator" } as? JLabel
                val likeLabel = panel.components.find { it.name == "likeIndicator" } as? JLabel

                if (playingLabel == null || likeLabel == null) {
                    panel.removeAll()
                    if (fileForPanel === currentMusic && isPlaying) {
                        panel.add(JLabel().apply { name = "playingIndicator"; icon = MusicIcons.playing }, BorderLayout.WEST)
                    } else {
                        panel.add(JLabel().apply { name = "playingIndicator" }, BorderLayout.WEST)
                    }
                    panel.add(JLabel(" " + fileForPanel.name), BorderLayout.CENTER)

                    val likeIndicatorToAdd = JLabel().apply { name = "likeIndicator" }
                    if (fileForPanel in currentLikeList) {
                        likeIndicatorToAdd.icon = MusicIcons.like
                    } else if (fileForPanel in currentDislikeList) {
                        likeIndicatorToAdd.icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
                    }
                    panel.add(likeIndicatorToAdd, BorderLayout.EAST)
                    panel.revalidate()
                    listNeedsRepaint = true
                    continue
                }

                var panelChanged = false

                val shouldHavePlayingIcon = (fileForPanel === currentMusic && isPlaying)
                val currentPlayingIcon = playingLabel.icon

                if (shouldHavePlayingIcon && currentPlayingIcon == null) {
                    playingLabel.icon = MusicIcons.playing
                    panelChanged = true
                } else if (!shouldHavePlayingIcon && currentPlayingIcon != null) {
                    playingLabel.icon = null
                    panelChanged = true
                }

                val expectedLikeStatusIcon: Icon? = when {
                    fileForPanel in currentLikeList -> MusicIcons.like
                    fileForPanel in currentDislikeList -> if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
                    else -> null
                }

                if (likeLabel.icon != expectedLikeStatusIcon) {
                    likeLabel.icon = expectedLikeStatusIcon
                    panelChanged = true
                }

                if (panelChanged) {
                    panel.revalidate()
                    listNeedsRepaint = true
                }
            } else {
                panel.removeAll()
                if (fileForPanel === currentMusic && isPlaying) {
                    panel.add(JLabel().apply { name = "playingIndicator"; icon = MusicIcons.playing }, BorderLayout.WEST)
                } else {
                    panel.add(JLabel().apply { name = "playingIndicator" }, BorderLayout.WEST)
                }
                panel.add(JLabel(" " + fileForPanel.name), BorderLayout.CENTER)

                val likeIndicatorToAdd = JLabel().apply { name = "likeIndicator" }
                if (fileForPanel in currentLikeList) {
                    likeIndicatorToAdd.icon = MusicIcons.like
                } else if (fileForPanel in currentDislikeList) {
                    likeIndicatorToAdd.icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
                }
                panel.add(likeIndicatorToAdd, BorderLayout.EAST)
                panel.revalidate()
                listNeedsRepaint = true
            }
        }

        if (listNeedsRepaint) {
            list.repaint()
        }
    }

    private fun chooseFolder() {
        val chooser = JFileChooser().apply { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY; isMultiSelectionEnabled = false }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            stopMusic()
            currentMusic = null
            selectedMusic = null
            displayMusicList(chooser.selectedFile)
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
            tempCover = chooser.selectedFile
            if (coverPanel.size.height != 0 && currentMusic === selectedMusic) {
                coverPanel.cover = ImageIcon(chooser.selectedFile.absolutePath)
                repaint()
            }
            refreshCoverButtonVisuals()
        }
    }

    private fun removeCover() {
        state.musicCoverMap.remove(selectedMusic!!.absolutePath)
        if (coverPanel.size.height != 0 && currentMusic === selectedMusic) {
            coverPanel.cover = null
            repaint()
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

            playButton.isEnabled = true
            prevButton.isEnabled = true
            nextButton.isEnabled = true
            likeButton.isEnabled = true
            coverButton.isEnabled = true

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
                            refreshCoverButtonVisuals()
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

            var processedName = if (state.beautifyTitleEnabled) when (state.beautifyTitle) {
                1 -> music.nameWithoutExtension.replace("_", " ")
                2 -> music.nameWithoutExtension.let { it.first().uppercase() + it.substring(1) }
                3 -> music.nameWithoutExtension.let { it.split("_").joinToString("") { it.first().uppercase() + it.substring(1) }}
                4 -> music.nameWithoutExtension.let { it.first().uppercase() + it.substring(1) }.replace("_", " ")
                5 -> music.nameWithoutExtension.let { it.split("_").joinToString(" ") { it.first().uppercase() + it.substring(1) }}
                else -> music.nameWithoutExtension
            } else music.name
            val playingLabel = JLabel().apply { name = "playingIndicator" }
            val nameLabel = JLabel(" $processedName")
            val likeLabel = JLabel().apply { name = "likeIndicator" }

            if (music === currentMusic && isPlaying) {
                playingLabel.icon = MusicIcons.playing
            }
            add(playingLabel, BorderLayout.WEST)

            add(nameLabel, BorderLayout.CENTER)

            if (music in currentLikeList) {
                likeLabel.icon = MusicIcons.like
            } else if (music in currentDislikeList) {
                likeLabel.icon = if (state.sensitiveIcon) MusicIcons.dislikeAnti else MusicIcons.dislike
            }
            add(likeLabel, BorderLayout.EAST)
        }
    }

    private fun playMusic() {
        alonePlayTime = 0
        if (defaultSettingHeight <= 0) (if (settingPanel.size.height <= 0) defaultSettingHeight = 268 else defaultSettingHeight = settingPanel.size.height)
        if (!isPlaying && currentMusic != null) {
            if (!switchMusic) showCover()
            else {
                val currentSelectedPath = selectedMusic?.absolutePath
                if (currentSelectedPath != null && state.musicCoverMap.containsKey(currentSelectedPath)) {
                    coverPanel.cover = ImageIcon(state.musicCoverMap[currentSelectedPath])
                } else {
                    coverPanel.cover = null
                }
            }
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
                        mp3Player?.setPlayBackListener(object : PlaybackListener() {
                            override fun playbackFinished(evt: PlaybackEvent?) {
                                if (evt?.source == mp3Player && isPlaying) {
                                    stopMusic()
                                    playCase()
                                }
                            }
                        })
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
                                    stopMusic()
                                    playCase()
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
                                    stopMusic()
                                    playCase()
                                }
                            }
                        }
                    }
                }
                isPlaying = true
                playButton.text = getExplainableMessage("button.stop.tooltip")
                playButton.icon = MusicIcons.stop
                refreshPlayingIconInList()
                revalidate()
                repaint()
            } catch (fnf: java.io.FileNotFoundException) {
                System.err.println("Play ERROR: File not found - ${currentMusic?.absolutePath} - ${fnf.message}")
                fnf.printStackTrace()
                stopMusic()
            } catch (e: Exception) {
                System.err.println("Play ERROR during setup or start: ${e.message}")
                e.printStackTrace()
                stopMusic()
                JOptionPane.showMessageDialog(this@HoshisukiUI, "Error starting playback: ${e.message}", "Playback Error", JOptionPane.ERROR_MESSAGE)
            }
        } else if (isPlaying) {
            stopMusic()
        }
    }

    private fun stopMusic() {
        val wasPlaying = isPlaying
        isPlaying = false

        if (!switchMusic) { 
            hideCover()
        }

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

        refreshPlayingIconInList()
        playButton.text = getExplainableMessage("button.play.tooltip")
        playButton.icon = MusicIcons.run
        playButton.isEnabled = musicFiles.isNotEmpty()

        revalidate()
        repaint()
    }

    fun JPanel.setHeight(height: Int) {
        preferredSize = Dimension(preferredSize.width, height)
    }

    private fun showCover() {
        val currentSelectedPath = selectedMusic?.absolutePath
        if (currentSelectedPath != null && state.musicCoverMap.containsKey(currentSelectedPath)) {
            coverPanel.cover = ImageIcon(state.musicCoverMap[currentSelectedPath])
        } else {
            coverPanel.cover = null
        }
        coverPanel.edgeLength = size.width
        settingPanel.setHeight(0)
        settingButton.isEnabled = true
        settingButton.isLatched = false
        revalidate()
        repaint()
    }

    private fun showSetting() {
        if (defaultSettingHeight > 0) {
            settingPanel.setHeight(defaultSettingHeight)
        } else {
            settingPanel.setHeight(settingPanel.preferredSize.height)
        }
        revalidate()
        repaint()
    }

    private fun hideCover() {
        coverPanel.edgeLength = 0
        if (defaultSettingHeight > 0) {
            settingPanel.setHeight(defaultSettingHeight)
        } else {
            settingPanel.setHeight(settingPanel.preferredSize.height)
        }
        settingButton.isLatched = true
        settingButton.isEnabled = false
        revalidate()
        repaint()
    }

    private fun hideSetting() {
        settingPanel.setHeight(0)
        revalidate()
        repaint()
    }

    private fun refreshAllButtonTooltips() {
        refreshLikeButtonVisuals()
        refreshCoverButtonVisuals()
        refreshPlayCaseButtonVisuals()
        playButton.text = if (isPlaying) getExplainableMessage("button.stop.tooltip") else getExplainableMessage("button.play.tooltip")
        nextButton.text = getExplainableMessage("button.next.tooltip")
        prevButton.text = getExplainableMessage("button.prev.tooltip")
        rescanButton.text = getExplainableMessage("button.rescan.tooltip")
        coverButton.text = getExplainableMessage(if (state.musicCoverMap.containsKey(selectedMusic?.absolutePath)) "button.remove.cover.tooltip" else "button.add.cover.tooltip")
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
                    playMusic()
                }
            }
            2 -> { // Alone Cycle
                if (currentMusic != null) {
                    playMusic()
                }
            }
            3 -> { // Alone Finite Cycle
                if (currentMusic != null && alonePlayTime < state.alonePlayTimes) {
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
            currentMusic = weightChooseMusic(chooseLike, true, true)
        } else if (!state.likeWeight.compare(0.0) && !state.dislikeWeight.compare(0.0)) {
            val chooseLike = if (state.likeWeight < 0) (Math.random() < (1 + state.likeWeight) * 0.1) else (Math.random() < state.likeWeight)
            val chooseDislike = if (state.dislikeWeight < 0) (Math.random() < (1 + state.dislikeWeight) * 0.1) else (Math.random() < state.dislikeWeight)
            currentMusic = weightChooseMusic(chooseLike, chooseDislike, false)
        } else throw RandomPlayException("Unknown ERROR: Illegal value")
        if (currentMusic == null) throw RandomPlayException("Unknown ERROR: currentMusic is null")
        if (recordPlayedMusic && !playedMusic.contains(currentMusic!!)) playedMusic.add(currentMusic!!)
        selectedMusic = currentMusic
        list.selectedIndex = musicFiles.indexOf(currentMusic)
        playMusic()
    }

    private fun weightChooseMusic(chooseLike: Boolean, chooseDislike: Boolean, withNormal: Boolean): File {
        var tempList = ArrayList<File>()
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

