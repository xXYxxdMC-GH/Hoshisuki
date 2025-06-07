package com.xxyxxdmc.icons;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

public interface MusicIcons {
    Icon folder = AllIcons.Nodes.Folder;
    Icon run = AllIcons.RunConfigurations.TestState.Run;
    Icon stop = AllIcons.Actions.Suspend;
    Icon playForward = AllIcons.Actions.Play_forward;
    Icon playLast = AllIcons.Actions.Play_last;
    Icon playBack = AllIcons.Actions.Play_back;
    Icon playFirst = AllIcons.Actions.Play_first;
    Icon listCycle = AllIcons.Actions.Refresh;
    Icon listReverseCycle = AllIcons.Actions.ToggleSoftWrap;
    Icon listPlay = AllIcons.RunConfigurations.Scroll_down;
    Icon listReversePlay = AllIcons.RunConfigurations.Scroll_up;
    Icon aloneCycle = AllIcons.Actions.Restart;
    Icon aloneCycleInTimes = AllIcons.Gutter.RecursiveMethod;
    Icon random = AllIcons.Nodes.Deploy;
    Icon randomInTimes = AllIcons.Nodes.Undeploy;
    Icon stopOnFinish = AllIcons.Actions.MoveToButton;
    Icon like = AllIcons.Actions.IntentionBulb;
    Icon unDislike = AllIcons.Actions.IntentionBulbGrey;
    Icon dislike = AllIcons.Actions.QuickfixBulb;
    Icon dislikeAnti = AllIcons.Actions.QuickfixOffBulb;
    Icon rescan = AllIcons.Actions.RestartFrame;
    Icon setting = AllIcons.Actions.InlayGear;
    Icon addCover = AllIcons.Debugger.AddToWatch;
    Icon removeCover = AllIcons.Actions.GC;
    Icon add = AllIcons.General.Add;
    Icon remove = AllIcons.Diff.Remove;
    Icon exchange = AllIcons.Diff.ApplyNotConflicts;
    Icon multiFolder = AllIcons.Actions.ModuleDirectory;
    Icon extend = AllIcons.Actions.Play_back;
    Icon foldUp = AllIcons.Actions.FindAndShowNextMatches;
    Icon playing = IconLoader.getIcon("/icons/library.svg", MusicIcons.class);
    Icon player = IconLoader.getIcon("/icons/hoshisuki.svg", MusicIcons.class);
}
