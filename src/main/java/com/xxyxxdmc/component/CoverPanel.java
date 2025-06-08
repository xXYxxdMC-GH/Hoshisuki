package com.xxyxxdmc.component;

import com.intellij.ui.JBColor;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

public final class CoverPanel extends JPanel {
    private ImageIcon cover;
    private int edgeLength;

    public CoverPanel(@Nullable ImageIcon cover, int edgeLength, int height) {
        if (cover != null) this.cover = cover;
        else {
            String themeName = UIManager.getLookAndFeel().getName();
            URL defaultCoverUrl = getClass().getClassLoader().getResource(String.format("icons/cover_%s.png", (themeName.contains("Darcula") || themeName.contains("Dark")) ? "dark" : "light"));
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
        //g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

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
            Image img = this.cover.getImage();
            int imgWidth = img.getWidth(this);
            int imgHeight = img.getHeight(this);

            if (imgWidth > 0 && imgHeight > 0) {
                double imgAspect = (double) imgWidth / imgHeight;
                double canvasAspect = (double) drawableWidth / drawableHeight;

                int drawImgWidth;
                int drawImgHeight;
                int x = padding;
                int y = padding;

                if (imgAspect > canvasAspect) {
                    drawImgHeight = drawableHeight;
                    drawImgWidth = (int) (drawImgHeight * imgAspect);
                    x = padding - (drawImgWidth - drawableWidth) / 2;
                } else {
                    drawImgWidth = drawableWidth;
                    drawImgHeight = (int) (drawImgWidth / imgAspect);
                    y = padding - (drawImgHeight - drawableHeight) / 2;
                }
                g2d.drawImage(img, x, y, drawImgWidth, drawImgHeight, this);
            }
        }

        g2d.setClip(null);
        g2d.setColor(JBColor.border());
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(roundedRectangle);

        g2d.dispose();
    }


    public void setCover(@Nullable ImageIcon cover) {
        if (cover == null) {
            String themeName = UIManager.getLookAndFeel().getName();
            URL defaultCoverUrl = getClass().getClassLoader().getResource(String.format("icons/cover_%s.png", (themeName.contains("Darcula") || themeName.contains("Dark")) ? "dark" : "light"));
            assert defaultCoverUrl != null;
            this.cover = new ImageIcon(defaultCoverUrl);
        }
        else this.cover = cover;
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
