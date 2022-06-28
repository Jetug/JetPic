package com.simplemobiletools.gallery.pro.data.jetug

import android.app.Activity
import androidx.exifinterface.media.ExifInterface
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getParentPath
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.data.extensions.dateTakensDB
import com.simplemobiletools.gallery.pro.data.models.DateTaken
import org.joda.time.DateTime
import java.io.File
import kotlin.collections.ArrayList

fun File.setLastModified(date: DateTime){
    this.setLastModified(date.toDate().time)
}

fun Activity.saveDateTakenToExif(paths: ArrayList<String>, showToasts: Boolean, callback: (() -> Unit)? = null) {
    try {
        if(paths.isEmpty()) return

        val datesTaken = ArrayList<DateTaken>()

        ensureBackgroundThread {
            for (path in paths){
                val file = File(path)
                val lastModified = file.lastModified()
                val time = DateTime(lastModified) //file.lastModified()
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

            callback?.invoke()
        }
    } catch (e: Exception) {
        if (showToasts) {
            showErrorToast(e)
        }
    }
}

fun getDateFromExif(path: String): String?{
    return ExifInterface(path).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: ExifInterface(path).getAttribute(ExifInterface.TAG_DATETIME)
}
