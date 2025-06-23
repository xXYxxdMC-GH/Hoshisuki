package com.xxyxxdmc.component;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListPanel extends JPanel {
    private final JPanel itemsContainer;
    private BoxPanel selectedPanel;

    private final List<ListSelectionListener> listeners = new CopyOnWriteArrayList<>();

    public ListPanel(DefaultListModel<JPanel> listModel) {
        setOpaque(true);
        setLayout(new BorderLayout());
        this.itemsContainer = new JPanel();
        this.itemsContainer.setLayout(new BoxLayout(this.itemsContainer, BoxLayout.Y_AXIS));
        add(this.itemsContainer, BorderLayout.NORTH);

        for (int i = 0; i < listModel.size(); i++) {
            JPanel item = listModel.getElementAt(i);
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            this.itemsContainer.add(item);
            item.setVisible(true);

            this.itemsContainer.revalidate();
            this.itemsContainer.repaint();
        }
    }

    public void refreshSelection(BoxPanel value) {
        for (ListSelectionListener listener : listeners) {
            listener.valueChanged(value);
        }
    }

    public void addListener(@NotNull ListSelectionListener listener) {
        this.listeners.add(listener);
    }

    public void removeListeners() {
        this.listeners.clear();
    }

    public void addItem(JPanel itemPanel) {
        itemPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        itemsContainer.add(itemPanel);

        itemsContainer.revalidate();
        itemsContainer.repaint();
    }

    public void setSelectedItem(BoxPanel newSelectedItem) {
        BoxPanel oldSelectedItem = this.selectedPanel;

        if (oldSelectedItem != newSelectedItem) {
            if (oldSelectedItem != null) {
                oldSelectedItem.setSelected(false);
            }
            this.selectedPanel = newSelectedItem;
            if (newSelectedItem != null) {
                newSelectedItem.setSelected(true);
            }
        }
    }

    public void setSelectedItem(BoxPanel newSelectedItem, boolean objectiveSelect) {
        setSelectedItem(newSelectedItem);
        refreshSelection(newSelectedItem);
    }

    public void setSelectedItem(File file) {
        for (Component component: itemsContainer.getComponents()) {
            if (component instanceof BoxPanel box) {
                if (box.getMusic() == file) {
                    setSelectedItem(box);
                    break;
                }
            }
        }
    }

    public BoxPanel getSelectedItem() {
        return this.selectedPanel;
    }
}
