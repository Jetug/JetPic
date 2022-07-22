package com.simplemobiletools.gallery.pro.data.jetug

import android.content.Context
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

fun Context.saveDateTakenToExif(paths: ArrayList<String>, showToasts: Boolean, callback: (() -> Unit)? = null) {
    if (paths.isEmpty()) return

    ensureBackgroundThread {
        try {
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

            callback?.invoke()
        } catch (e: Exception) {
            if (showToasts) {
                showErrorToast(e)
            }
        }
    }
}

fun getDateFromExif(path: String): String?{
    return ExifInterface(path).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?: ExifInterface(path).getAttribute(ExifInterface.TAG_DATETIME)
}
