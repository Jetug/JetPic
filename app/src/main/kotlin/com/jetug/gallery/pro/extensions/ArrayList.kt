package com.jetug.gallery.pro.extensions

import com.jetug.gallery.pro.helpers.*
import com.jetug.gallery.pro.models.*

fun ArrayList<Medium>.getDirMediaTypes(): Int {
    var types = 0
    if (any { it.isImage() }) {
        types += TYPE_IMAGES
    }

    if (any { it.isVideo() }) {
        types += TYPE_VIDEOS
    }

    if (any { it.isGIF() }) {
        types += TYPE_GIFS
    }

    if (any { it.isRaw() }) {
        types += TYPE_RAWS
    }

    if (any { it.isSVG() }) {
        types += TYPE_SVGS
    }

    if (any { it.isPortrait() }) {
        types += TYPE_PORTRAITS
    }

    return types
}

fun ArrayList<ThumbnailItem>.getMediums(): ArrayList<Medium>{
    val result = this.takeWhile { it is Medium } as List<Medium>
    return ArrayList(result)
}

fun ArrayList<FolderItem>.getDirectories(): ArrayList<Directory>{
    val result = arrayListOf<Directory>()
    this.forEach{ item ->
        if (item is Directory)
            result.add(item)
        else if(item is DirectoryGroup){
            val dirs = item.innerDirs.clone() as ArrayList<Directory>
            dirs.forEach{d -> d.groupName = "" }
            result.addAll(dirs)
        }
    }
    return result
}

fun <T>ArrayList<T>.takeLast(): T{
    val item = this.last()
    this.removeAt(this.size - 1)
    return item
}
