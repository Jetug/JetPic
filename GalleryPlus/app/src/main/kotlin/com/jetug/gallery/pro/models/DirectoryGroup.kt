package com.jetug.gallery.pro.models

import androidx.room.Ignore

class DirectoryGroup(id: Long?, path: String, tmb: String, name: String, mediaCnt: Int, modified: Long,
                     taken: Long, size: Long, location: Int, types: Int, sortValue: String
) : FolderItem(id, path, tmb, name, mediaCnt, modified,
    taken,
    size,
    location,
    types, sortValue
) {
    @Ignore
    val innerDirs: ArrayList<Directory> = arrayListOf()
}
