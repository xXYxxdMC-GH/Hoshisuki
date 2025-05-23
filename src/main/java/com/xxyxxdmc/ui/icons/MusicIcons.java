package com.xxyxxdmc.ui.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;


public interface MusicIcons {
    Icon folder = AllIcons.Toolwindows.ToolWindowProject;
    Icon run = AllIcons.Toolwindows.ToolWindowRun;
    Icon pause = AllIcons.Actions.Pause;
    Icon playForward = AllIcons.Actions.Play_forward;
    Icon playBack = AllIcons.Actions.Play_back;
    Icon listCycle = AllIcons.Actions.Refresh;
    Icon listReserveCycle = AllIcons.Actions.ToggleSoftWrap;
    Icon listPlay = AllIcons.RunConfigurations.Scroll_down;
    Icon listReservePlay = AllIcons.RunConfigurations.Scroll_up;
    Icon aloneCycle = AllIcons.Actions.Restart;
    Icon aloneCycleInTimes = AllIcons.Gutter.RecursiveMethod;
    Icon random = AllIcons.Nodes.Deploy;
    Icon randomInTimes = AllIcons.Nodes.Undeploy;
    Icon playing = AllIcons.ObjectBrowser.ShowLibraryContents;
    Icon player = IconLoader.getIcon("/icons/hoshisuki.svg", MusicIcons.class);
}
