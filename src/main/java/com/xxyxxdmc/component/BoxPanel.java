package com.xxyxxdmc.component;

import javax.swing.*;
import java.io.File;

public class BoxPanel extends JPanel {
    private boolean selected = false;
    private File music;

    protected boolean isSelected() {
        return selected;
    }

    protected void setSelected(boolean selected) {
        this.selected = selected;
    }

    public File getMusic() {
        return music;
    }

    public void setMusic(File music) {
        this.music = music;
    }
}
