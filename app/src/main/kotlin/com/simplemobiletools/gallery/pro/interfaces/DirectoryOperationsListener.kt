package com.simplemobiletools.gallery.pro.interfaces

import com.simplemobiletools.gallery.pro.models.FolderItem
import java.io.File

interface DirectoryOperationsListener {
    fun refreshItems()

    fun deleteFolders(folders: ArrayList<File>)

    fun recheckPinnedFolders()

    fun updateDirectories(directories: ArrayList<FolderItem>)
}
