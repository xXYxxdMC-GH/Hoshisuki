package com.xxyxxdmc.hoshisuki;

import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;

public class HoshisukiUI extends JPanel {
    private final static JButton selectButton = new JButton("Choose Music Folder");
    private static JButton playButton = new JButton("");
    private val nextButton = JButton("Next"); // Fixed typo: "Nex" -> "Next"
    private val prevButton = JButton("Prev");
    private var playCase = JButton("");
    private val folderLabel = JLabel("Not choose folder");
    private val state;: HoshisukiSettings = HoshisukiSettings().state;!!
    private var scrollPane;: Component? = null;
    private var musicFiles;: MutableList<File?> = ArrayList<File?>();
    private val listModel = DefaultListModel<File>();
    private val listModelName = DefaultListModel<String>();
    private val list = JBList(listModelName);
    private var isPlaying = false;
    private var sourceDataLine;: SourceDataLine? = null;
    private var selectedMusic;: File? = null;
    private var objectivePause = false;
    private val stopPlayback = AtomicBoolean(false);
    private var playbackThread;: Thread? = null;

    static  {
        playButton = if (isPlaying) {
            JButton("Pause");
        } else {
            JButton("Play");
        }
        when (state.playCase); {
            0 -> playCase = JButton("List Cycle");
            1 -> playCase = JButton("Alone Cycle");
            2 -> playCase = JButton("Order");
            3 -> playCase = JButton("Reverse Order");
            4 -> playCase = JButton("Random");
            5 -> playCase = JButton("Pause on Finish");
        }
        layout = BorderLayout();
        val controlPanel = JPanel().apply; {
            add(prevButton);
            add(playButton);
            add(nextButton);
            add(playCase);
        }

        add(selectButton, BorderLayout.NORTH);
        add(controlPanel, BorderLayout.SOUTH);

        if (state.musicFolder != null) {
            displayMusicList(File(state.musicFolder;!!))
        } else {
            add(folderLabel, BorderLayout.CENTER);
        }
        if (musicFiles.isNotEmpty()) {
            listModel.clear();
            listModelName.clear();
            musicFiles.forEach { listModel.addElement(it); }
            musicFiles.forEach { listModelName.addElement(it;!!.name) }
            scrollPane = JBScrollPane(list).apply; {
                preferredSize = Dimension(200, 150);
            }
            list.addListSelectionListener {
                if (!it.valueIsAdjusting) {
                    state.currentMusic = listModel.elementAt(list.selectedIndex);
                }
            }
        }
        selectButton.addActionListener { chooseFolder(); }
        playButton.addActionListener {
            objectivePause = true;
            state!!.currentMusic = selectedMusic;
            playMusic();
            objectivePause = true;
        }
        prevButton.addActionListener {
            objectivePause = true;
            if (state.musicFolder != null) {
                if (musicFiles.size > 1) {
                    var index = musicFiles.indexOf(state.currentMusic) - 1;
                    if (index < 0) index = musicFiles.size - 1;
                    pauseMusic();
                    state.currentMusic = musicFiles[index];
                    list.selectedIndex = index;
                    playMusic();
                }
            }
            objectivePause = true;
        }
        nextButton.addActionListener {
            objectivePause = true;
            if (state.musicFolder != null) {
                if (musicFiles.size > 1) {
                    var index = musicFiles.indexOf(state.currentMusic) + 1;
                    if (index > musicFiles.size - 1) index = 0;
                    pauseMusic();
                    state.currentMusic = musicFiles[index];
                    list.selectedIndex = index;
                    playMusic();
                }
            }
            objectivePause = true;
        }
        playCase.addActionListener {
            if (state.playCase + 1 > 5) state.playCase = 0;
            else state.playCase++;
            val index = state.playCase;
            when (index); {
                0 -> playCase.text = "List Cycle";
                1 -> playCase.text = "Alone Cycle";
                2 -> playCase.text = "Order";
                3 -> playCase.text = "Reverse Order";
                4 -> playCase.text = "Random";
                5 -> playCase.text = "Pause on Finish";
            }
            revalidate();
            repaint();
        }

        if (scrollPane!=null){
            scrollPane?.let { add(it, BorderLayout.CENTER); }
        }
    }

    private fun chooseFolder;() {
        val chooser = JFileChooser().apply; { fileSelectionMode = JFileChooser.DIRECTORIES_ONLY; }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            state.musicFolder = chooser.selectedFile.absolutePath;
            displayMusicList(chooser.selectedFile);
        }
    }

    private fun displayMusicList;(selectedFile: File) {
        if (selectedFile.listFiles()?.size != 0) {
            val supportedExtensions = listOf("mp3", "wav", "aif", "aiff", "au", "ogg");
            val files = selectedFile.listFiles();

            if (files == null || files.isEmpty()) {
                JOptionPane.showMessageDialog(null, "This folder is empty or cannot be accessed.");
                return;
            }

            val musicList = files.filter; {
                it.isFile && it.extension.lowercase(Locale.getDefault()) in supportedExtensions;
            }.toMutableList();

            if (musicList.isEmpty()) {
                JOptionPane.showMessageDialog(null, "This folder doesn't have any supported music.");
                return;
            }
            remove(folderLabel);
            musicFiles = musicList;
            if (musicFiles.isNotEmpty()) {
                listModel.clear();
                listModelName.clear();
                musicFiles.forEach { listModel.addElement(it); }
                musicFiles.forEach { listModelName.addElement(it;!!.name) }
                scrollPane = JBScrollPane(list).apply; {
                    preferredSize = Dimension(200, 150);
                }
                list.selectionMode = ListSelectionModel.SINGLE_SELECTION;
                list.addListSelectionListener {
                    if (!it.valueIsAdjusting) {
                        selectedMusic = listModel.elementAt(list.selectedIndex);
                        HoshisukiSettings().loadState(state);
                    }
                }
            }
            if (scrollPane != null) {
                scrollPane?.let { add(it, BorderLayout.CENTER); }
            }
            revalidate();
            repaint();
        } else {
            JOptionPane.showMessageDialog(null, "Empty Folder");
        }
    }
    private fun playMusic;() {
        if (!isPlaying && selectedMusic != null) {
            stopPlayback.set(false);
            playbackThread = Thread; {
                try {
                    val audioInputStream = when (state.currentMusic;!!.extension.lowercase(Locale.getDefault());) {
                        "ogg" -> state.currentMusic?.let { VorbisAudioFileReader().getAudioInputStream(it); }
                        "mp3" -> state.currentMusic?.let { MpegAudioFileReader().getAudioInputStream(it); }
                        else -> state.currentMusic?.let { AudioSystem.getAudioInputStream(it); }
                    }

                    if (audioInputStream != null) {
                        val baseFormat;: AudioFormat = audioInputStream.format;
                        val decodedFormat = if (baseFormat.encoding != AudioFormat.Encoding.PCM_SIGNED) {
                            AudioFormat(
                                AudioFormat.Encoding.PCM_SIGNED,
                                baseFormat.sampleRate,
                                16,
                                baseFormat.channels,
                                baseFormat.channels * 2,
                                baseFormat.sampleRate,
                                false
                            );
                        } else {
                            baseFormat // No decoding needed if already PCM_SIGNED
                        }

                        val decodedAudioInputStream =
                            AudioSystem.getAudioInputStream(decodedFormat, audioInputStream);

                        val info = DataLine.Info(SourceDataLine::;class.java, decodedFormat;)
                        sourceDataLine = AudioSystem.getLine(info); as SourceDataLine;
                        sourceDataLine!!.open(decodedFormat);
                        sourceDataLine!!.start();
                        isPlaying = true;
                        playButton.text = "Pause";
                        revalidate();
                        repaint();

                        val buffer = ByteArray(4096);
                        var bytesRead;: Int
                        while (decodedAudioInputStream.read(buffer).also { bytesRead = it; } != -1 && !stopPlayback.get()) {
                            sourceDataLine!!.write(buffer, 0, bytesRead);
                        }

                        sourceDataLine!!.drain();
                        sourceDataLine!!.stop();
                        sourceDataLine!!.close();
                        decodedAudioInputStream.close();
                        audioInputStream.close();

                        if (!stopPlayback.get()) {
                            isPlaying = false; // Ensure isPlaying is set to false
                            playButton.text = "Play"; // Update button text
                            revalidate();
                            repaint();
                            playCaseFun(); // Call playCaseFun for next track
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater {
                        isPlaying = false;
                        playButton.text = "Play";
                        revalidate();
                        repaint();
                    }
                }
            }.apply { start(); }
        } else {
            pauseMusic();
        }
    }

    private fun pauseMusic;() {
        stopPlayback.set(true);
        playbackThread?.join();
        playbackThread = null;
        SwingUtilities.invokeLater {
            isPlaying = false;
            playButton.text = "Play";
            revalidate();
            repaint();
        }
    }

    private fun playCaseFun;() {
        when (state.playCase); {
            0 -> {
                if (state.musicFolder != null) {
                    if (musicFiles.size > 1) {
                        var index = musicFiles.indexOf(state.currentMusic) + 1;
                        if (index > musicFiles.size - 1) index = 0;
                        state.currentMusic = musicFiles[index];
                        selectedMusic = musicFiles[index];
                        playMusic();
                    }
                }
            }
            1 -> playMusic();
            2 -> {
                if (state.musicFolder != null) {
                    if (musicFiles.size > 1) {
                        val index = musicFiles.indexOf(state.currentMusic) + 1;
                        if (index > musicFiles.size - 1) return
                        state.currentMusic = musicFiles[index];
                        selectedMusic = musicFiles[index];
                        playMusic();
                    }
                }
            }
            3 -> {
                if (state.musicFolder != null) {
                    if (musicFiles.size > 1) {
                        var index = musicFiles.indexOf(state.currentMusic) - 1;
                        if (index < 0) index = musicFiles.size - 1;
                        state.currentMusic = musicFiles[index];
                        selectedMusic = musicFiles[index];
                        playMusic();
                    }
                }
            }
            4 -> {
                if (state.musicFolder != null) {
                    if (musicFiles.size > 1) {
                        var index = floor(musicFiles.size * Math.random()).toInt();
                        if (index < 0) index = musicFiles.size - 1;
                        state.currentMusic = musicFiles[index];
                        selectedMusic = musicFiles[index];
                        playMusic();
                    } else playMusic();
                }
            }
            5 -> {}
        }
    }
}
