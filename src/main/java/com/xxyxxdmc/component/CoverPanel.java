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
    private int height;

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

        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padding = 10;
        int imageWidth = edgeLength;
        int imageHeight = (height == -1) ? edgeLength : height;

        int arcRadius = 20;
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                padding, padding, imageWidth, imageHeight, arcRadius, arcRadius
        );

        g2d.setClip(roundedRectangle);
        if (cover != null && cover.getImage() != null) {
            g2d.drawImage(cover.getImage(), padding, padding, imageWidth, imageHeight, this);
        }

        g2d.setColor(JBColor.BLACK);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(roundedRectangle);

        g2d.dispose();
    }

    public void setCover(ImageIcon cover) {
        this.cover = cover;
        setPreferredSize(new Dimension(edgeLength, edgeLength));
        revalidate();
        repaint();
    }

    public void setEdgeLength(int edgeLength) {
        this.edgeLength = edgeLength;
        setPreferredSize(new Dimension(this.edgeLength, this.edgeLength));
        revalidate();
        repaint();
    }

    public void setHeight(int height) {
        this.height = height;
        setPreferredSize(new Dimension(this.edgeLength, this.height));
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
