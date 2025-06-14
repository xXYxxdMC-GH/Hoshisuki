package com.xxyxxdmc.component;

import com.xxyxxdmc.icons.MusicIcons;

import javax.swing.*;
import java.awt.*;

public class MusicPanel extends JPanel {
    private boolean playing;
    private JLabel playingLabel;
    private boolean blank;
    private Component blankPlace;
    private final JLabel nameLabel;
    private IconTooltipActionButton likeButton;

    public MusicPanel(boolean playing, boolean blank, String musicName, int likeInfo, Runnable action) {
        setLayout(new BorderLayout());
        this.playing = playing;
        this.blank = blank;
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
        this.blankPlace = Box.createHorizontalStrut(7);
        add(this.blankPlace, BorderLayout.WEST);
        this.playingLabel = new JLabel(" ");
        this.playingLabel.setIcon(MusicIcons.playing);
        add(this.playingLabel, BorderLayout.WEST);
        add(this.nameLabel, BorderLayout.CENTER);
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BorderLayout());
        panel1.add(Box.createHorizontalStrut(2), BorderLayout.WEST);
        panel1.add(Box.createHorizontalStrut(2), BorderLayout.EAST);
        panel1.add(this.likeButton, BorderLayout.CENTER);
        add(panel1, BorderLayout.EAST);
        playingLabel.setVisible(playing);
        blankPlace.setVisible(blank);
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isBlank() {
        return blank;
    }

    public void setBlank(boolean blank) {
        this.blank = blank;
    }

    public JLabel getPlayingLabel() {
        return playingLabel;
    }

    public void setPlayingLabel(JLabel playingLabel) {
        this.playingLabel = playingLabel;
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

    public void setBlankPlace(Component blankPlace) {
        this.blankPlace = blankPlace;
    }

    public Runnable getAction() {
        return likeButton.getAction();
    }

    public void setAction(Runnable action) {
        likeButton.setAction(action);
    }
}
