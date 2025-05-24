package com.xxyxxdmc.ui.component;

import com.intellij.openapi.actionSystem.ActionButtonComponent;
import com.intellij.openapi.actionSystem.ex.ActionButtonLook;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.ClickListener;
// No longer assuming it extends ScalableIconComponent from IntelliJ
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable; // Import for @Nullable

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class IconTooltipActionButton extends JComponent { // Extends JComponent
    private boolean hovered = false;
    private Icon myIcon;
    private String myTooltip;
    private Runnable myAction;

    public IconTooltipActionButton(@NotNull Icon icon, @NlsContexts.Tooltip @Nullable String tooltipText, @Nullable final Runnable action) {
        this.myIcon = icon;
        this.myTooltip = tooltipText;
        this.myAction = action;

        // Set initial preferred size based on the icon and padding
        // This is now handled by the overridden getPreferredSize()
        // but setting it here ensures an initial size before first paint.
        setPreferredSize(getPreferredSize());


        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                repaint();
            }
        });

        new ClickListener() {
            @Override
            public boolean onClick(@NotNull MouseEvent e, int clickCount) {
                if (myAction != null) {
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

        // 1. Paint hover effect background FIRST (if hovered)
        if (hovered) {
            ActionButtonLook.SYSTEM_LOOK.paintBackground(g, this, ActionButtonComponent.SELECTED);
        }

        // 2. Then paint the icon on TOP of the background
        if (myIcon != null) {
            int x = (getWidth() - myIcon.getIconWidth()) / 2;
            int y = (getHeight() - myIcon.getIconHeight()) / 2;
            myIcon.paintIcon(this, g, x, y);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (myIcon != null) {
            int padding = 4; // Example padding
            return new Dimension(myIcon.getIconWidth() + padding * 2, myIcon.getIconHeight() + padding * 2);
        }
        return super.getPreferredSize(); // Fallback to default or what was set
    }


    public Icon getIcon() {
        return myIcon;
    }

    public void setIcon(@NotNull Icon icon) {
        this.myIcon = icon;
        Dimension oldPreferredSize = getPreferredSize();
        this.myIcon = icon; // Set the new icon
        Dimension newPreferredSize = getPreferredSize();

        if (!oldPreferredSize.equals(newPreferredSize)) {
            setPreferredSize(newPreferredSize); // Explicitly set if it changes
            revalidate(); // Important if size changes
        } else {
            revalidate(); // Still good to call if content changes but size might not
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
}