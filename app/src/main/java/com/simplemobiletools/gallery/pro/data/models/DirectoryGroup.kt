package com.simplemobiletools.gallery.pro.data.models

import androidx.room.Ignore
import com.simplemobiletools.gallery.pro.data.helpers.LOCATION_INTERNAL

class DirectoryGroup(id: Long?, path: String, tmb: String, name: String, mediaCnt: Int, modified: Long,
                     taken: Long, size: Long, location: Int //, types: Int
) : FolderItem(id, path, tmb, name, mediaCnt, modified,
    taken,
    size,
    location,
    //types
) {

    constructor(dir: Directory, groupName: String) : this (null,
        dir.path,
        dir.tmb,
        groupName,
        dir.mediaCnt,
        dir.modified,
        dir.taken,
        dir.size,
        LOCATION_INTERNAL
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
