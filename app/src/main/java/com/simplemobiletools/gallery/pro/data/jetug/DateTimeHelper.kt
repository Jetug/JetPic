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
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.io.File
import kotlin.collections.ArrayList

fun File.setLastModified(date: DateTime){
    this.setLastModified(date.toDate().time)
}

fun Context.saveDateToExif(paths: ArrayList<String>, showToasts: Boolean, callback: (() -> Unit)? = null) {
    if (paths.isEmpty()) return

    IOScope.launch {
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
        } catch (e: Exception) {
            if (showToasts) {
                showErrorToast(e)
            }
        }
    }
}


