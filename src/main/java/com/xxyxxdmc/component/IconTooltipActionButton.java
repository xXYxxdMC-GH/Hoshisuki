package com.xxyxxdmc.component;

import com.intellij.openapi.actionSystem.ActionButtonComponent;
import com.intellij.openapi.actionSystem.ex.ActionButtonLook;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.ClickListener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class IconTooltipActionButton extends JComponent {
    private boolean hovered = false;
    private Icon myIcon;
    private String myTooltip;
    private final boolean myLatch;
    private boolean latched;
    private Runnable myAction;

    public IconTooltipActionButton(@NotNull Icon icon, @NlsContexts.Tooltip @Nullable String tooltipText, @Nullable final Runnable action) {
        this(icon, tooltipText, false, action);
    }

    public IconTooltipActionButton(@NotNull Icon icon, @NlsContexts.Tooltip @Nullable String tooltipText, boolean latch, @Nullable final Runnable action) {
        this.myIcon = icon;
        this.myTooltip = tooltipText;
        this.myAction = action;
        this.myLatch = latch;
        this.latched = false;

        setPreferredSize(getPreferredSize());

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isEnabled()) return;
                hovered = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!isEnabled()) return;
                hovered = false;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                repaint();
            }
        });

        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent e, int clickCount) {
                if (!isEnabled()) return false;
                if (myAction != null) {
                    if (myLatch) latched = !latched;
                    myAction.run();
                    return true;
                }
                return false;
            }
        }.installOn(this);

        if (tooltipText != null) {
            this.setToolTipText(tooltipText);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (hovered) {
            ActionButtonLook.SYSTEM_LOOK.paintBackground(g, this, ActionButtonComponent.SELECTED);
        }
        if (latched) {
            ActionButtonLook.SYSTEM_LOOK.paintBackground(g, this, ActionButtonComponent.PUSHED);
        }

        if (myIcon != null) {
            int x = (getWidth() - myIcon.getIconWidth()) / 2;
            int y = (getHeight() - myIcon.getIconHeight()) / 2;
            myIcon.paintIcon(this, g, x, y);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (myIcon != null) {
            int padding = 4;
            return new Dimension(myIcon.getIconWidth() + padding * 2, myIcon.getIconHeight() + padding * 2);
        }
        return super.getPreferredSize();
    }

    public Icon getIcon() {
        return myIcon;
    }

    public void setIcon(@NotNull Icon icon) {
        this.myIcon = icon;
        Dimension oldPreferredSize = getPreferredSize();
        this.myIcon = icon;
        Dimension newPreferredSize = getPreferredSize();

        if (!oldPreferredSize.equals(newPreferredSize)) {
            setPreferredSize(newPreferredSize);
            revalidate();
        } else {
            revalidate();
        }
        repaint();
    }

    public void setText(@NlsContexts.Tooltip @Nullable String tooltip) {
        this.myTooltip = tooltip;
        this.setToolTipText(tooltip);
    }

    @Nullable
    public String getText() {
        return this.myTooltip;
    }

    public void setAction(@Nullable Runnable action) {
        this.myAction = action;
    }

    @Nullable
    public Runnable getAction() {
        return this.myAction;
    }

    public boolean isLatched() {
        return latched;
    }
}