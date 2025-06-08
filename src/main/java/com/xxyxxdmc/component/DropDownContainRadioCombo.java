package com.xxyxxdmc.component;

import com.intellij.openapi.ui.ComboBox;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public final class DropDownContainRadioCombo<E> extends ComboBox<E> {
    private ArrayList<JPanel> comboBoxes;
    private ArrayList<JPanel> radioButtons;

    public DropDownContainRadioCombo(String[] comboBoxNames, String[] radioButtonNames) {
        for (String comboBox : comboBoxNames) {
            JPanel comboBoxPanel = new JPanel();
            comboBoxPanel.setLayout(new BorderLayout());
            JCheckBox checkBox = new JCheckBox();
            checkBox.addActionListener(e -> {
                if (checkBox.isSelected()) {

                }
            });
            comboBoxPanel.add(new JCheckBox(), BorderLayout.WEST);
            comboBoxPanel.add(new JLabel(comboBox), BorderLayout.CENTER);
            this.comboBoxes.add();
        }
    }
}
