package com.simplemobiletools.gallery.pro.data.extensions

import android.webkit.MimeTypeMap
import java.io.File

val File.isPhotoVideo: Boolean get() = mime.startsWith("image/") || mime.startsWith("video/")

val File.mime: String get() = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: ""

fun File.move(targetDir: String) {
    if (this.exists() && File(targetDir).isDirectory)
        renameTo(File(targetDir, name))
}
