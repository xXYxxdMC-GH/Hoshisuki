package com.xxyxxdmc.musicplayer.data

import java.io.File

object FileScanner {
    fun scanMusicFiles(folderPath: String): List<File> {
        val folder = File(folderPath)
        return folder.listFiles { file -> file.extension in listOf("mp3", "wav") }?.toList() ?: emptyList()
    }
}