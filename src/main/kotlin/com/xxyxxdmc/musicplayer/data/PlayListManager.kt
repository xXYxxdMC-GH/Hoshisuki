package com.xxyxxdmc.musicplayer.data

import java.io.File

class PlaylistManager {
    private val playlists = mutableMapOf<String, List<File>>()

    fun addPlaylist(name: String, files: List<File>) {
        playlists[name] = files
    }

    fun getPlaylist(name: String): List<File>? {
        return playlists[name]
    }
}
