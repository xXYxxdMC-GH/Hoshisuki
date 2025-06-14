package com.xxyxxdmc.component;

import com.xxyxxdmc.icons.MusicIcons;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FolderPanel extends JPanel {
    private boolean playing;
    private boolean state;
    private IconTooltipActionButton folderButton;
    private IconTooltipActionButton controlButton;

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
        add(this.folderButton, BorderLayout.WEST);
        this.controlButton = new IconTooltipActionButton(
                state ? MusicIcons.foldUp : MusicIcons.extend,
                null, action
        );
        add(new JLabel(folderPath), BorderLayout.CENTER);
        add(this.controlButton, BorderLayout.EAST);
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
        this.folderButton.setIcon(playing ? MusicIcons.playingFolder : MusicIcons.multiFolder);
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
        this.controlButton.setIcon(state ? MusicIcons.foldUp : MusicIcons.extend);
    }

    public Runnable getAction() {
        return this.controlButton.getAction();
    }

    public void setAction(Runnable action) {
        this.controlButton.setAction(action);
    }

    public IconTooltipActionButton getFolderButton() {
        return this.folderButton;
    }

    public void setFolderButton(IconTooltipActionButton folderButton) {
        this.folderButton = folderButton;
    }

    public IconTooltipActionButton getControlButton() {
        return this.controlButton;
    }

    public void setControlButton(IconTooltipActionButton controlButton) {
        this.controlButton = controlButton;
    }
}
