package com.xxyxxdmc.component;

import com.xxyxxdmc.icons.MusicIcons;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FolderPanel extends JPanel {
    private boolean playing;
    private boolean state;
    private final IconTooltipActionButton folderButton;
    private final IconTooltipActionButton controlButton;

    public FolderPanel(boolean playing, String folderPath, String text, boolean state, Runnable action) {
        setLayout(new BorderLayout());
        this.playing = playing;
        this.state = state;
        this.folderButton = new IconTooltipActionButton(
                playing ? MusicIcons.playingFolder : MusicIcons.multiFolder,
                text, () -> {
                    try {
                        Desktop.getDesktop().open(new File(folderPath));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(Box.createHorizontalStrut(2), BorderLayout.WEST);
        panel1.add(Box.createHorizontalStrut(2), BorderLayout.EAST);
        panel1.add(this.folderButton, BorderLayout.CENTER);
        add(panel1, BorderLayout.WEST);
        this.controlButton = new IconTooltipActionButton(
                state ? MusicIcons.foldUp : MusicIcons.extend,
                null, action
        );
        add(new JLabel(folderPath));
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout());
        panel2.add(Box.createHorizontalStrut(2), BorderLayout.WEST);
        panel2.add(Box.createHorizontalStrut(2), BorderLayout.EAST);
        panel2.add(this.controlButton, BorderLayout.CENTER);
        add(panel2, BorderLayout.EAST);
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
        this.folderButton.setIcon(playing ? MusicIcons.playingFolder : MusicIcons.multiFolder);
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
        this.controlButton.setIcon(state ? MusicIcons.foldUp : MusicIcons.extend);
    }
}
