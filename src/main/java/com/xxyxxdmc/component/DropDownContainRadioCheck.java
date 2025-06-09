package com.xxyxxdmc.component;

import org.jetbrains.annotations.Nullable; // Keep if you use @Nullable, otherwise remove

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class DropDownContainRadioCheck extends JComboBox<JPanel> {
    private final ArrayList<JPanel> checkBoxPanels;
    private final ArrayList<JPanel> radioButtonPanels;
    private final ArrayList<JPanel> allItems; // Holds all panels for the ComboBoxModel
    private final ArrayList<Runnable> actions;

    public DropDownContainRadioCheck(List<String> checkNames, List<String> radioButtonNames, List<Runnable> actions) {
        if (checkNames == null) throw new IllegalArgumentException("checkNames cannot be null");
        if (radioButtonNames == null) throw new IllegalArgumentException("radioButtonNames cannot be null");
        if (actions == null) throw new IllegalArgumentException("actions cannot be null");

        this.actions = new ArrayList<>(actions);
        this.checkBoxPanels = new ArrayList<>();
        this.radioButtonPanels = new ArrayList<>();
        this.allItems = new ArrayList<>();

        ButtonGroup radioButtonGroup = new ButtonGroup();

        int actionCounter = 0;

        // Create CheckBox items
        for (String checkName : checkNames) {
            JPanel panel = new JPanel(new BorderLayout(5, 0)); // BorderLayout with horizontal gap
            panel.setOpaque(true); // Important for renderer to control background

            JCheckBox checkBox = new JCheckBox();
            final int currentActionIndex = actionCounter;
            if (currentActionIndex < this.actions.size()) {
                checkBox.addActionListener(e -> this.actions.get(currentActionIndex).run());
            }
            actionCounter++;

            panel.add(checkBox, BorderLayout.WEST);
            panel.add(new JLabel(checkName), BorderLayout.CENTER);
            // Make the panel itself clickable to toggle the checkbox (optional UX improvement)
            panel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    checkBox.setSelected(!checkBox.isSelected());
                    // Manually trigger action if needed, as programmatic change might not fire ActionListener
                    if (checkBox.getActionListeners().length > 0) {
                        checkBox.getActionListeners()[0].actionPerformed(
                                new java.awt.event.ActionEvent(checkBox, java.awt.event.ActionEvent.ACTION_PERFORMED, null)
                        );
                    }
                }
            });


            this.checkBoxPanels.add(panel);
            this.allItems.add(panel);
        }

        // Create RadioButton items
        for (String radioName : radioButtonNames) {
            JPanel panel = new JPanel(new BorderLayout(5, 0)); // BorderLayout with horizontal gap
            panel.setOpaque(true); // Important for renderer to control background

            JRadioButton radioButton = new JRadioButton();
            radioButtonGroup.add(radioButton); // Add to ButtonGroup for mutual exclusivity

            final int currentActionIndex = actionCounter;
            if (currentActionIndex < this.actions.size()) {
                radioButton.addActionListener(e -> {
                    // Radio button actions typically run when it becomes selected
                    if (radioButton.isSelected()) {
                        this.actions.get(currentActionIndex).run();
                    }
                });
            }
            actionCounter++;

            panel.add(radioButton, BorderLayout.WEST);
            panel.add(new JLabel(radioName), BorderLayout.CENTER);
            // Make the panel itself clickable to select the radio button (optional UX improvement)
            panel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    radioButton.setSelected(true); // This will trigger its ActionListener
                }
            });

            this.radioButtonPanels.add(panel);
            this.allItems.add(panel);
        }

        // Set the custom model and renderer
        setModel(new PanelComboBoxModel(this.allItems));
        setRenderer(new PanelRenderer());

        // By default, JComboBox might try to make the selected item editable as text.
        // For panel items, you usually don't want this.
        // However, the renderer handles the display of the selected item.
        // setEditable(false); // Consider this if you face issues with editing.
    }

    /**
     * Custom ComboBoxModel to hold JPanel items.
     */
    private static class PanelComboBoxModel extends AbstractListModel<JPanel> implements ComboBoxModel<JPanel> {
        private final List<JPanel> items;
        private JPanel selectedItem;

        public PanelComboBoxModel(List<JPanel> items) {
            this.items = new ArrayList<>(items); // Use a copy
        }

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public JPanel getElementAt(int index) {
            if (index >= 0 && index < items.size()) {
                return items.get(index);
            }
            return null;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            // JComboBox may pass null or one of the items.
            if (anItem == null || anItem instanceof JPanel) {
                if (selectedItem != anItem) { // Only update if selection actually changes
                    selectedItem = (JPanel) anItem;
                    fireContentsChanged(this, -1, -1); // Notify JComboBox of selection change
                }
            }
            // If anItem is not a JPanel and not null, it might be an issue,
            // but with a proper renderer and items, this should mostly be JPanels.
        }

        @Override
        public @Nullable Object getSelectedItem() {
            return selectedItem;
        }
    }

    /**
     * Custom ListCellRenderer to display JPanels in the dropdown.
     */
    private static class PanelRenderer implements ListCellRenderer<JPanel> {
        @Override
        public Component getListCellRendererComponent(JList<? extends JPanel> list,
                                                      JPanel value, // This is our JPanel item
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            if (value == null) {
                // Handle null case, e.g., when the model is empty or an item is null
                return new JLabel(" "); // Or some other placeholder
            }

            // The 'value' is the JPanel itself.
            // We can apply selection highlighting to the panel's background.
            // Children components like JCheckBox, JRadioButton, JLabel will inherit opaque panel's background
            // if they are not opaque themselves or if their background is not explicitly set.
            // For consistent look, ensure child components are not opaque or set their background too.
            if (isSelected) {
                value.setBackground(list.getSelectionBackground());
                // Set foreground for all JLabels within the panel
                for(Component c : value.getComponents()){
                    if(c instanceof JLabel){
                        c.setForeground(list.getSelectionForeground());
                    }
                    c.setBackground(list.getSelectionBackground()); // Ensure children also get selection background
                }
            } else {
                value.setBackground(list.getBackground());
                for(Component c : value.getComponents()){
                    if(c instanceof JLabel){
                        c.setForeground(list.getForeground());
                    }
                    c.setBackground(list.getBackground());
                }
            }
            return value; // Return the panel itself to be rendered.
        }
    }

    // Example Usage (optional, for testing)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("DropDown Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            ArrayList<String> checks = new ArrayList<>();
            checks.add("Option A");
            checks.add("Option B");
            checks.add("Option C");
            ArrayList<String> radios = new ArrayList<>();
            radios.add("Radio 1");
            radios.add("Radio 2");
            radios.add("Radio 3");
            ArrayList<Runnable> actions = new ArrayList<>();
            for (String name : checks) {
                actions.add(() -> System.out.println(name + " toggled/action!"));
            }
            for (String name : radios) {
                actions.add(() -> System.out.println(name + " selected/action!"));
            }

            DropDownContainRadioCheck customCombo = new DropDownContainRadioCheck(checks, radios, actions);
            customCombo.setPreferredSize(new Dimension(200, 30)); // Set preferred size for the combo box itself

            // To make the dropdown list wider than the combo box
            // customCombo.setPrototypeDisplayValue("A very long item name to make popup wider"); // One way
            // Or, more robustly, you might need to customize the popup itself if using JComboBox directly.
            // If you were using com.intellij.openapi.ui.ComboBoxWithWidePopup, it handles this.

            frame.add(customCombo);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}