package com.jetug.gallery.pro.models.jetug

import android.os.Build
import androidx.annotation.RequiresApi
import com.jetug.commons.helpers.*
import com.jetug.gallery.pro.models.FolderItem
import com.jetug.gallery.pro.models.Medium
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

@RequiresApi(Build.VERSION_CODES.O)
fun sortDirs(dirs:ArrayList<FolderItem>, sorting: Int): ArrayList<FolderItem>{
    if (sorting and SORT_BY_RANDOM != 0) {
        dirs.shuffle()
        return dirs
    }

    fun reverse(): ArrayList<FolderItem> {
        if(sorting and SORT_DESCENDING != 0) dirs.reverse()
        return dirs
    }

    dirs.sortWith(Comparator { o1, o2 ->
        o1 as FolderItem
        o2 as FolderItem

        val result = when{
            sorting and SORT_BY_NAME != 0  -> {
                AlphanumericComparator().compare(o1.name.lowercase(), o2.name.lowercase()) //Locale.getDefault()
            }
            sorting and SORT_BY_DATE_TAKEN != 0 -> {
                val path1 = FileSystems.getDefault().getPath(File(o1.path).absolutePath)
                val path2 = FileSystems.getDefault().getPath(File(o2.path).absolutePath)
                val attr1 = Files.readAttributes(path1, BasicFileAttributes::class.java)
                val attr2 = Files.readAttributes(path2, BasicFileAttributes::class.java)
                (attr1.creationTime()).compareTo(attr2.creationTime())
            }
            sorting and SORT_BY_DATE_MODIFIED != 0 -> {
                (o1.modified).compareTo(o2.modified)
            }
            sorting and SORT_BY_PATH != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.path.lowercase(), o2.path.lowercase())
                } else {
                    o1.path.lowercase().compareTo(o2.path.lowercase())
                }
            }
            sorting and SORT_BY_SIZE != 0 -> (o1.size).compareTo(o2.size)
            else -> (o1.name.toLongOrNull() ?: 0).compareTo(o2.name.toLongOrNull() ?: 0)
        }
        return@Comparator result
    })
    reverse()
    return dirs
}

fun sortMedia(media: ArrayList<Medium>, sorting: Int): ArrayList<Medium> {
    if (sorting and SORT_BY_RANDOM != 0) {
        media.shuffle()
        return media
    }

    media.sortWith { o1, o2 ->
        o1 as Medium
        o2 as Medium
        var result = when {
            sorting and SORT_BY_NAME != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.name.toLowerCase(), o2.name.toLowerCase())
                } else {
                    o1.name.toLowerCase().compareTo(o2.name.toLowerCase())
                }
            }
            sorting and SORT_BY_PATH != 0 -> {
                if (sorting and SORT_USE_NUMERIC_VALUE != 0) {
                    AlphanumericComparator().compare(o1.path.toLowerCase(), o2.path.toLowerCase())
                } else {
                    o1.path.toLowerCase().compareTo(o2.path.toLowerCase())
                }
            }
            sorting and SORT_BY_SIZE != 0 -> o1.size.compareTo(o2.size)
            sorting and SORT_BY_DATE_MODIFIED != 0 -> o1.modified.compareTo(o2.modified)
            else -> o1.taken.compareTo(o2.taken)
        }

        if (sorting and SORT_DESCENDING != 0) {
            result *= -1
        }
        result
    }

    return media
}
