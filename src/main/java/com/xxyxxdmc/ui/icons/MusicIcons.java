package com.xxyxxdmc.ui.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;


public interface MusicIcons {
    Icon folder = AllIcons.Toolwindows.ToolWindowProject;
    Icon run = AllIcons.Actions.RunAll;
    Icon pause = AllIcons.Actions.Pause;
    Icon next = AllIcons.Actions.Play_forward;
    Icon playBack = AllIcons.Actions.Play_back;
    Icon listCycle = AllIcons.General.InlineRefresh;
    Icon aloneCycle = AllIcons.Actions.Restart;
    Icon player = IconLoader.getIcon("/icons/hoshisuki.svg", MusicIcons.class);
}
