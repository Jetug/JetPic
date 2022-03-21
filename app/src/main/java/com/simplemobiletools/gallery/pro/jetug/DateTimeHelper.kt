package com.simplemobiletools.gallery.pro.jetug

import android.app.Activity
import androidx.exifinterface.media.ExifInterface
import com.simplemobiletools.commons.extensions.getFilenameFromPath
import com.simplemobiletools.commons.extensions.getParentPath
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.gallery.pro.extensions.dateTakensDB
import com.simplemobiletools.gallery.pro.models.DateTaken
import org.joda.time.DateTime
import java.io.File
import kotlin.collections.ArrayList

//fun changeFileDate(file: File, date: DateTime){
//    file.setLastModified(date.toDate().time)
//}

fun File.setLastModified(date: DateTime){
    this.setLastModified(date.toDate().time)
}

fun Activity.saveDateTakenToExif(paths: ArrayList<String>, showToasts: Boolean, callback: (() -> Unit)? = null) {
    try {
        if(paths.isEmpty()) return

        val dateTakens = java.util.ArrayList<DateTaken>()

        ensureBackgroundThread {
            for (path in paths){
                val file = File(path)
                val lastModified = file.lastModified()
                val time = DateTime(lastModified) //file.lastModified()

                //2018:09:05 15:09:05
                val stringTime = time.toString("yyyy:MM:dd hh:mm:ss")
                //toast(stringTime)
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
                dateTakens.add(dateTaken)
            }

            if (dateTakens.isNotEmpty()) {
                dateTakensDB.insertAll(dateTakens)
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
