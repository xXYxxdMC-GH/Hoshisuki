package com.xxyxxdmc.component;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

public class CoverPanel extends JPanel {
    private ImageIcon cover;
    private int edgeLength;

    public CoverPanel(@Nullable ImageIcon cover, int edgeLength, int height) {
        if (cover != null) this.cover = cover;
        else {
            URL defaultCoverUrl = getClass().getClassLoader().getResource("icons/cover.png");
            assert defaultCoverUrl != null;
            this.cover = new ImageIcon(defaultCoverUrl);
        }
        this.edgeLength = edgeLength;
        setPreferredSize(new Dimension(edgeLength, (height == -1) ? edgeLength : height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int currentWidth = getWidth();
        int currentHeight = getHeight();

        if (currentWidth <= 0 || currentHeight <= 0) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padding = 10;
        int drawableWidth = currentWidth - padding * 2;
        int drawableHeight = currentHeight - padding * 2;

        if (drawableWidth <= 0 || drawableHeight <= 0) {
            g2d.dispose();
            return;
        }

        int arcRadius = 20;
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                padding, padding, drawableWidth, drawableHeight, arcRadius, arcRadius
        );

        g2d.setClip(roundedRectangle);

        if (this.cover != null && this.cover.getImage() != null && this.cover.getImageLoadStatus() == MediaTracker.COMPLETE) {
            g2d.drawImage(this.cover.getImage(), padding, padding, drawableWidth, drawableHeight, this);
        } else {
            g2d.setColor(JBColor.LIGHT_GRAY);
            g2d.fillRect(padding, padding, drawableWidth, drawableHeight);
            g2d.setColor(JBColor.DARK_GRAY);
            g2d.drawString("No Cover", padding + 5, padding + 20);
        }

        g2d.setClip(null);
        g2d.setColor(JBColor.border());
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(roundedRectangle);

        g2d.dispose();
    }


    public void setCover(ImageIcon cover) {
        this.cover = cover;
        setPreferredSize(new Dimension(edgeLength, edgeLength));
        repaint();
    }

    public void setEdgeLength(int edgeLength) {
        this.edgeLength = edgeLength;
        setPreferredSize(new Dimension(this.edgeLength, this.edgeLength));
        revalidate();
        repaint();
    }

    public void setHeight(int height) {
        setPreferredSize(new Dimension(this.edgeLength, height));
        revalidate();
        repaint();
    }

    public ImageIcon getCover() {
        return cover;
    }

    public int getEdgeLength() {
        return edgeLength;
    }
}
