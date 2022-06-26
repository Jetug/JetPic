package com.simplemobiletools.gallery.pro.data.models

import androidx.room.Ignore

class DirectoryGroup(id: Long?, path: String, tmb: String, name: String, mediaCnt: Int, modified: Long,
                     taken: Long, size: Long, location: Int, types: Int, sortValue: String
) : FolderItem(id, path, tmb, name, mediaCnt, modified,
    taken,
    size,
    location,
    types, sortValue
) {

    constructor(dir: Directory, groupName: String) : this (null,
        dir.path,
        dir.tmb,
        groupName,
        dir.mediaCnt,
        dir.modified,
        dir.taken,
        dir.size,
        0,
        dir.types,
        dir.sortValue
    )

    override var size: Long = 0
        get(){
            var size = 0L
            innerDirs.forEach { size+=it.size }
            return size
    }

    override var mediaCnt: Int = 0
        get(){
            var cnt = 0
            innerDirs.forEach { cnt+=it.mediaCnt }
            return cnt
        }

    @Ignore
    val innerDirs: ArrayList<Directory> = arrayListOf()
}
