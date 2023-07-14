package com.simplemobiletools.gallery.pro.data.jetug

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile

fun Context.getFileNameFromUri(uri: Uri): String? {
    val document = DocumentFile.fromSingleUri(applicationContext, uri)
    return if (document != null && !document.isDirectory) return document.name else ""
}

fun Context.getDirectoryNameFromUri(uri: Uri): String {
    val displayName = DocumentFile.fromTreeUri(this, uri)?.name

    if (displayName != null) return displayName

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        return cursor.getString(nameIndex)
    }

    return ""
}
