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
