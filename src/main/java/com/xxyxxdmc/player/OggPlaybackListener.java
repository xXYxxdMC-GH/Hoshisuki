package com.xxyxxdmc.player;

public interface OggPlaybackListener {
    void onPlaybackStopped(String filePath, boolean dueToError);
    void onPlaybackFinished(String filePath);
    void onPlaybackError(String filePath, OggPlayerException e);
}
