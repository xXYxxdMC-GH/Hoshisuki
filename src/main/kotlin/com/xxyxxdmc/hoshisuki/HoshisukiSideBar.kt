package com.xxyxxdmc.hoshisuki

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.annotations.NotNull


class HoshisukiSideBar: ToolWindowFactory {
    override fun createToolWindowContent(project: Project, @NotNull toolWindow: ToolWindow) {
        toolWindow.contentManager.addContent(
            ContentFactory.getInstance().createContent(HoshisukiUI(), "", false))
    }
}