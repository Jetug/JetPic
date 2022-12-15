package com.simplemobiletools.gallery.pro.data.jetug

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getParentPath
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.IOScope
import com.simplemobiletools.gallery.pro.data.extensions.context.dateTakensDB
import com.simplemobiletools.gallery.pro.data.models.DateTaken
import com.simplemobiletools.gallery.pro.data.models.Medium
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.io.File
import kotlin.collections.ArrayList

fun File.setLastModified(date: DateTime){
    this.setLastModified(date.toDate().time)
}

fun Context.saveDateToExif(paths: ArrayList<String>, showToasts: Boolean, callback: (() -> Unit) = {}) = IOScope.launch {
    if (paths.isEmpty()) return@launch

    try {
        toast(R.string.save_exif)
        val datesTaken = ArrayList<DateTaken>()

        for (path in paths) {
            val file = File(path)
            val lastModified = file.lastModified()
            val time = DateTime(lastModified)
            val stringTime = time.toString("yyyy:MM:dd hh:mm:ss")
            val exif = ExifInterface(path)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, stringTime)
            exif.setAttribute(ExifInterface.TAG_DATETIME, stringTime)
            exif.saveAttributes()
            file.setLastModified(lastModified)

            val dateTaken = DateTaken(
                null,
                path,
                path.getFilenameFromPath(),
                path.getParentPath(),
                lastModified,
                (System.currentTimeMillis() / 1000).toInt(),
                lastModified
            )
            datesTaken.add(dateTaken)
        }

        if (datesTaken.isNotEmpty()) {
            dateTakensDB.insertAll(datesTaken)
        }

        toast(R.string.save_exif_success)
        callback?.invoke()
    }
    catch (e: Exception) {
        if (showToasts) {
            showErrorToast(e)
        }
    }
}

//            var date: Long = list[i + 1].modified
//            var dateTime = DateTime(date)
//
//            dateTime = if (isUp)
//                dateTime.minusMillis(1)
//            else
//                dateTime.plusMillis(1)
//
//            list[i].modified = dateTime.toDate().time;//

fun Context.alignDate(list: ArrayList<Medium>, callback: (() -> Unit) = {}) {
    val list2 = list.clone() as ArrayList<Medium>
    val list = list.clone() as ArrayList<Medium>

    if(list.isEmpty() || list.size == 1)
        return

    val isUp = list.isAscending()

    val dates = dateTakensDB.getAllDateTakens()
    val newDates = arrayListOf<DateTaken>()

    for (i in 0 .. list.lastIndex){
        if(i == 0){
            list[i].modified = add(list[i + 1].modified, 1, !isUp)
        }
        else if(i == list.lastIndex){
            list[i].modified = add(list[i - 1].modified, 1, isUp)
        }
        else if(list[i - 1].modified > list[i].modified == isUp){
            list[i].modified = add(list[i - 1].modified, 1, isUp)
        }
        else continue

        File(list[i].path).setLastModified(list[i].modified)

        dates.forEach {
            if (list[i].path == it.fullPath){
                val date = it
                date.lastModified = list[i].modified
                newDates.add(date)
            }
        }
    }

    dateTakensDB.insertAll(newDates);
    callback()
}

fun add(a: Long, b: Long, arg: Boolean): Long = if (arg) a + b else a - b

fun ArrayList<Medium>.isAscending(): Boolean{
    var up   = 0
    var down = 0

    for (i in 0 until this.size){
        if (i == this.size - 1) continue

        val curr = this[  i  ]
        val next = this[i + 1]

        if(curr.modified < next.modified) up++
        else down++
    }
    return up > down
}
