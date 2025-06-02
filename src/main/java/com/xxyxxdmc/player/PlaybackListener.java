package com.xxyxxdmc.player;

public interface PlaybackListener {
    void onPlaybackStarted(String filePath);
    void onPlaybackStopped(String filePath, boolean dueToError); // Indicates if stop was due to error or user request
    void onPlaybackFinished(String filePath); // Successfully reached end of stream
    void onPlaybackError(String filePath, OggPlayerException e);
    void onProgressUpdate(String filePath, long currentMicroseconds, long totalMicroseconds);
}
