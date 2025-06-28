package com.xxyxxdmc.component;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.xxyxxdmc.icons.MusicIcons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class FolderPanel extends BoxPanel {
    private boolean playing;
    private boolean state;
    private IconTooltipActionButton folderButton;
    private IconTooltipActionButton controlButton;

    private static final Color DEFAULT_BACKGROUND = UIUtil.getLabelBackground();
    private static final Color SELECTED_BACKGROUND = UIUtil.getListSelectionBackground(true);
    private static final Color HOVER_BACKGROUND = new JBColor(Color.GRAY, Color.DARK_GRAY);

    public FolderPanel(boolean playing, String folderPath, String text, boolean state, Runnable action, Runnable action2) {
        setLayout(new BorderLayout());
        updateBackground();
        super.setSelected(false);
        this.playing = playing;
        this.state = state;
        this.folderButton = new IconTooltipActionButton(
                playing ? MusicIcons.playingFolder : MusicIcons.multiFolder,
                text, false, () -> {
                    try {
                        Desktop.getDesktop().open(new File(folderPath));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, action2
        );
        super.setMusic(new File(folderPath));
        add(this.folderButton, BorderLayout.WEST);
        this.controlButton = new IconTooltipActionButton(
                state ? MusicIcons.foldUp : MusicIcons.extend,
                null, action
        );
        add(new JLabel(folderPath), BorderLayout.CENTER);
        add(this.controlButton, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (getParent().getParent() instanceof ListPanel listPanel) {
                    listPanel.setSelectedItem(FolderPanel.this);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!FolderPanel.super.isSelected()) {
                    setBackground(HOVER_BACKGROUND);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!FolderPanel.super.isSelected()) {
                    setBackground(DEFAULT_BACKGROUND);
                    repaint();
                }
            }
        });
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        updateBackground();
    }

    private void updateBackground() {
        if (isSelected()) {
            setBackground(SELECTED_BACKGROUND);
        } else {
            setBackground(DEFAULT_BACKGROUND);
        }
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
