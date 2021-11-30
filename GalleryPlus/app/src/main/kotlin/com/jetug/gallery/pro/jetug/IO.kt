package com.jetug.gallery.pro.jetug

import org.joda.time.DateTime
import java.io.File


fun changeFileDate(file: File, date: DateTime){
    file.setLastModified(date.toDate().time)
}

fun changeFileDate(path: String, date: DateTime){
    File(path).setLastModified(date.toDate().time)
}
