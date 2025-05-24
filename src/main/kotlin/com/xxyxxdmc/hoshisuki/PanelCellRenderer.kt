package com.xxyxxdmc.hoshisuki

import java.awt.Component
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer


class PanelCellRenderer : ListCellRenderer<JPanel> {
    override fun getListCellRendererComponent(
        list: JList<out JPanel>?,
        value: JPanel?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        if (isSelected) {
            value?.background = list?.selectionBackground
            value?.foreground = list?.selectionForeground
        } else {
            value?.background = list?.background
            value?.foreground = list?.foreground
        }
        return value ?: JPanel()
    }
}