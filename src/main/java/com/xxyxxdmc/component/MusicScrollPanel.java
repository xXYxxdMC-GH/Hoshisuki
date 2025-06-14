package com.xxyxxdmc.component;

import javax.swing.*;
import java.awt.*;

public class MusicScrollPanel extends JPanel {
    private final ScrollView scrollView;
    private final JScrollBar verticalScrollBar;

    public MusicScrollPanel(ScrollView scrollView) {
        this.scrollView = scrollView;
        setLayout(new BorderLayout());

        add(scrollView, BorderLayout.CENTER);
        verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
        add(verticalScrollBar, BorderLayout.EAST);

        verticalScrollBar.setMinimum(0);
        verticalScrollBar.setMaximum(scrollView.getPreferredSize().height);
        verticalScrollBar.setVisibleAmount(getHeight());

        verticalScrollBar.addAdjustmentListener(e -> {
            int newY = e.getValue();
            scrollView.setLocation(scrollView.getX(), -newY);
            scrollView.revalidate();
            scrollView.repaint();
        });
        scrollView.addMouseWheelListener(e -> {
            int scrollAmount = e.getUnitsToScroll() * scrollView.getScrollableUnitIncrement(scrollView.getVisibleRect(), JScrollBar.VERTICAL, e.getWheelRotation());
            int newValue = verticalScrollBar.getValue() + scrollAmount;
            verticalScrollBar.setValue(Math.max(verticalScrollBar.getMinimum(), Math.min(verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount(), newValue)));
        });
    }

    @Override
    public void doLayout() {
        super.doLayout();
        int contentHeight = scrollView.getPreferredSize().height;
        verticalScrollBar.setMaximum(contentHeight);

        int viewportHeight = scrollView.getHeight();
        verticalScrollBar.setVisibleAmount(viewportHeight);

        if (verticalScrollBar.getValue() + verticalScrollBar.getVisibleAmount() > verticalScrollBar.getMaximum()) {
            verticalScrollBar.setValue(verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount());
        }
        if (verticalScrollBar.getValue() < verticalScrollBar.getMinimum()) {
            verticalScrollBar.setValue(verticalScrollBar.getMinimum());
        }

        scrollView.setLocation(scrollView.getX(), -verticalScrollBar.getValue());
    }
}
