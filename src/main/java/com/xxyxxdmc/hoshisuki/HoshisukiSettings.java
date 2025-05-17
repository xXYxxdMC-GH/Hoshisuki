package com.xxyxxdmc.hoshisuki;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.File;

@State(name = "HoshisukiSettings", storages = {@Storage("HoshisukiSettings.xml")})
public class HoshisukiSettings implements PersistentStateComponent<HoshisukiSettings> {
    String musicFolder = null;
    File currentMusic = null;
    int playCase = 0;

    @Nullable
    @Override
    public HoshisukiSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull HoshisukiSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static final class Companion {
        public final HoshisukiSettings getInstance() {
            return ApplicationManager.getApplication().getService(HoshisukiSettings.class);
        }
    }
}
