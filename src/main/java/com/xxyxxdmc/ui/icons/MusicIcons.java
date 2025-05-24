package com.xxyxxdmc.ui.icons;

import com.intellij.icons.ExpUiIcons;
import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

public interface MusicIcons {
    Icon folder = ExpUiIcons.Nodes.Folder;
    Icon run = ExpUiIcons.Gutter.Run;
    Icon pause = ExpUiIcons.Run.Pause;
    Icon stop = ExpUiIcons.Run.Stop;
    Icon playForward = ExpUiIcons.Actions.PlayForward;
    Icon playBack = ExpUiIcons.Actions.PlayBack;
    Icon listCycle = ExpUiIcons.General.Refresh;
    Icon listReverseCycle = ExpUiIcons.General.SoftWrap;
    Icon listPlay = ExpUiIcons.General.ScrollDown;
    Icon listReversePlay = ExpUiIcons.General.ScrollUp;
    Icon aloneCycle = ExpUiIcons.Run.Restart;
    Icon aloneCycleInTimes = ExpUiIcons.Gutter.RecursiveMethod;
    Icon random = ExpUiIcons.Actions.Deploy;
    Icon randomInTimes = ExpUiIcons.Actions.Undeploy;
    Icon pauseOnFinish = ExpUiIcons.Actions.MoveToButton;
    Icon playing = ExpUiIcons.Nodes.Library;
    Icon like = ExpUiIcons.CodeInsight.IntentionBulb;
    Icon unlike = ExpUiIcons.CodeInsight.IntentionBulbGrey;
    Icon dislike = ExpUiIcons.CodeInsight.QuickfixBulb;
    Icon dislikeAnti = ExpUiIcons.CodeInsight.QuickfixOffBulb;
    Icon player = IconLoader.getIcon("/icons/hoshisuki.svg", MusicIcons.class);
}
