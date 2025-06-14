package com.xxyxxdmc.component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ScrollView extends JPanel implements Scrollable {
    private Dimension preferredScrollableViewportSize;
    private int selectedIndex = -1;
    private DefaultListModel<JPanel> model;

    public ScrollView(DefaultListModel<JPanel> listModel) {
        this.model = listModel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        for (int i = 0;i<listModel.getSize();i++) {
            JPanel panel = listModel.getElementAt(i);
            panel.setOpaque(true);
            add(panel);
        }
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int oldSelectedIndex = selectedIndex;
                selectedIndex = -1;
                for (int i = 0; i < getComponentCount(); i++) {
                    Component c = getComponent(i);
                    if (c instanceof JPanel && c.getBounds().contains(e.getPoint())) {
                        selectedIndex = i;
                        break;
                    }
                }
                if (oldSelectedIndex != -1 && oldSelectedIndex < getComponentCount()) {
                    getComponent(oldSelectedIndex).repaint();
                }
                if (selectedIndex != -1 && selectedIndex < getComponentCount()) {
                    getComponent(selectedIndex).repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (selectedIndex != -1 && selectedIndex < getComponentCount()) {
            Component selectedComponent = getComponent(selectedIndex);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(UIManager.getColor("List.selectionBackground"));
            g2d.fillRect(selectedComponent.getX(), selectedComponent.getY(), selectedComponent.getWidth(), selectedComponent.getHeight());
            g2d.dispose();
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return preferredScrollableViewportSize;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height - 20;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void setPreferredScrollableViewportSize(Dimension preferredScrollableViewportSize) {
        this.preferredScrollableViewportSize = preferredScrollableViewportSize;
    }
}
