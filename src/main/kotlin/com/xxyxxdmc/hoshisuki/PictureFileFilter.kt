package com.xxyxxdmc.hoshisuki

import java.io.File
import java.util.Locale
import javax.swing.filechooser.FileFilter

class PictureFileFilter: FileFilter() {
    override fun accept(f: File): Boolean {
        if (f.isDirectory) return true
        val extension = f.extension.lowercase(Locale.getDefault())

        return extension in listOf("jpg", "jpeg", "png", "gif")
    }

    override fun getDescription(): String {
        return HoshisukiBundle.message("file.filter.picture.description")
    }
}