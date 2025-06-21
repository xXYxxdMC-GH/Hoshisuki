package com.xxyxxdmc.component;

import javax.swing.*;
import java.io.File;

public class BoxPanel extends JPanel {
    private boolean selected = false;
    private File music;

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public File getMusic() {
        return music;
    }

    public void setMusic(File music) {
        this.music = music;
    }
}
