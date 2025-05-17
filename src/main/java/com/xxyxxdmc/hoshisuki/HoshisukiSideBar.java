package com.xxyxxdmc.hoshisuki;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class HoshisukiSideBar implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(new HoshisukiUI(), "", false);
        toolWindow.getContentManager().addContent(content);
        // Assuming HoshisukiIcons.Hoshisuki is a Java class with a static field
        // If it's Kotlin, you might need to access it differently (e.g., through a companion object)
        // toolWindow.setIcon(HoshisukiIcons.Hoshisuki); // Uncomment and adjust if needed
    }
}