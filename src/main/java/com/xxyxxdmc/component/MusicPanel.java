package com.xxyxxdmc.component;

import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import com.xxyxxdmc.icons.MusicIcons;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class MusicPanel extends BoxPanel {
    private boolean playing;
    private final JLabel playingLabel;
    private Component blankPlace;
    private final JLabel nameLabel;
    private IconTooltipActionButton likeButton;

    private static final Color DEFAULT_BACKGROUND = UIUtil.getLabelBackground();
    private static final Color SELECTED_BACKGROUND = UIUtil.getListSelectionBackground(true);
    private static final Color HOVER_BACKGROUND = new JBColor(Color.GRAY, Color.DARK_GRAY);

    public MusicPanel(String path, String musicName, int likeInfo, Runnable action) {
        setLayout(new BorderLayout());
        updateBackground();
        super.setMusic(new File(path));
        super.setSelected(false);
        this.playing = false;
        this.likeButton = new IconTooltipActionButton(
                switch (likeInfo) {
                    case 1 -> MusicIcons.like;
                    case 2 -> MusicIcons.dislike;
                    case 3 -> MusicIcons.dislikeAnti;
                    default -> MusicIcons.unDislike;
                },
                null, action
        );
        this.nameLabel = new JLabel(musicName);
        this.blankPlace = new JLabel("     ");
        add(this.blankPlace, BorderLayout.WEST);
        this.playingLabel = new JLabel(" ");
        this.playingLabel.setIcon(MusicIcons.playing);
        add(this.nameLabel, BorderLayout.CENTER);
        add(this.likeButton, BorderLayout.EAST);
        if (this.likeButton.getIcon() == MusicIcons.unDislike) this.likeButton.setIcon(MusicIcons.empty);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (getParent().getParent() instanceof ListPanel listPanel) {
                    listPanel.setSelectedItem(MusicPanel.this);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!MusicPanel.super.isSelected()) {
                    setBackground(HOVER_BACKGROUND);
                }
                if (likeButton.getIcon() == MusicIcons.empty) likeButton.setIcon(MusicIcons.unDislike);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!MusicPanel.super.isSelected()) {
                    setBackground(DEFAULT_BACKGROUND);
                }
                if (likeButton.getIcon() == MusicIcons.unDislike) likeButton.setIcon(MusicIcons.empty);
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
            repaint();
        } else {
            setBackground(DEFAULT_BACKGROUND);
            repaint();
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
        if (playing) this.add(this.playingLabel, BorderLayout.WEST);
        else this.remove(this.playingLabel);
    }

    public JLabel getNameLabel() {
        return nameLabel;
    }

    public IconTooltipActionButton getLikeButton() {
        return likeButton;
    }

    public void setLikeButton(IconTooltipActionButton likeButton) {
        this.likeButton = likeButton;
    }

    public Component getBlankPlace() {
        return blankPlace;
    }

    public void setBlankPlace(JLabel blankPlace) {
        this.blankPlace = blankPlace;
    }

    public Runnable getAction() {
        return likeButton.getAction();
    }

    public void setAction(Runnable action) {
        likeButton.setAction(action);
    }
}
