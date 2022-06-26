package com.simplemobiletools.gallery.pro.data.interfaces

import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem

interface MediaOperationsListener {
    fun refreshItems()

    fun tryDeleteFiles(fileDirItems: ArrayList<FileDirItem>)

    fun selectedPaths(paths: ArrayList<String>)

    fun updateMediaGridDecoration(media: ArrayList<ThumbnailItem>)
}
