package com.jetug.gallery.pro.interfaces

import com.jetug.gallery.pro.models.Directory
import com.jetug.gallery.pro.models.FolderItem
import java.io.File

interface DirectoryOperationsListener {
    fun refreshItems()

    fun deleteFolders(folders: ArrayList<File>)

    fun recheckPinnedFolders()

    fun updateDirectories(directories: ArrayList<FolderItem>)
}
