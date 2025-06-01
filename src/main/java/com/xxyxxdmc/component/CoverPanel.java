package com.xxyxxdmc.component;

import com.intellij.ui.JBColor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CoverPanel extends JPanel {
    private ImageIcon cover;
    private int edgeLength;
    private int padding = 10;
    private int arcRadius = 20;

    public CoverPanel(ImageIcon cover, int edgeLength) {
        this.cover = cover;
        this.edgeLength = edgeLength;
        setPreferredSize(new Dimension(edgeLength, edgeLength));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 转换为 Graphics2D 以使用更高级的绘图功能
        Graphics2D g2d = (Graphics2D) g.create(); // 创建副本以避免修改原始 Graphics 对象

        // 开启抗锯齿，使圆角更平滑
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int imageX = padding;
        int imageY = padding;
        int imageWidth = edgeLength;
        int imageHeight = edgeLength;

        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                imageX, imageY, imageWidth, imageHeight, arcRadius, arcRadius
        );

        g2d.setClip(roundedRectangle);
        if (cover != null && cover.getImage() != null) {
            g2d.drawImage(cover.getImage(), imageX, imageY, imageWidth, imageHeight, this);
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
        setPreferredSize(new Dimension(this.edgeLength + 2 * padding, this.edgeLength + 2 * padding));
        revalidate();
        repaint();
    }

    public ImageIcon getCover() {
        return cover;
    }

    public int getEdgeLength() {
        return edgeLength;
    }

    public void setPadding(int padding) {
        this.padding = padding;
        setPreferredSize(new Dimension(edgeLength + 2 * this.padding, edgeLength + 2 * this.padding));
        revalidate();
        repaint();
    }

    public int getPadding() {
        return padding;
    }

    public void setArcRadius(int arcRadius) {
        this.arcRadius = arcRadius;
        repaint();
    }

    public int getArcRadius() {
        return arcRadius;
    }
}
