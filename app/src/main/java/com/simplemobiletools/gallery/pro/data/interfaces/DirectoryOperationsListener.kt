package com.simplemobiletools.gallery.pro.data.interfaces

import com.simplemobiletools.gallery.pro.data.models.FolderItem
import java.io.File

interface DirectoryOperationsListener {
    fun refreshItems()

    fun deleteFolders(folders: ArrayList<File>)

    fun recheckPinnedFolders()

    fun updateDirectories(directories: ArrayList<FolderItem>)
}
