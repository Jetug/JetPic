package com.simplemobiletools.gallery.pro.data.jetug.workers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.data.models.tasks.SimpleTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun Context.createMediaMoveTask(sourcePath: Uri, destinationPath: Uri) {
    val inputData = Data.Builder()
        .putString(KEY_SOURCE_PATH, sourcePath.toString())
        .putString(KEY_DESTINATION_PATH, destinationPath.toString())
        .build()

    val mediaMoveWorkRequest = PeriodicWorkRequestBuilder<MediaMoveWorker>(15, TimeUnit.MINUTES)
        .setInputData(inputData)
        .addTag("DirectoryRedirect")
        .build()

    val id = mediaMoveWorkRequest.id
    val name = getDirectoryNameFromUri(sourcePath) + " to " + getDirectoryNameFromUri(destinationPath)

    config.addTask(SimpleTask(id.toString(), name))
    WorkManager.getInstance(this).enqueue(mediaMoveWorkRequest)
}

fun Context.getFileNameFromUri(uri: Uri): String? {
    val document = DocumentFile.fromSingleUri(applicationContext, uri)
    return if (document != null && !document.isDirectory) return document.name else ""
}

fun Context.getDirectoryNameFromUri(uri: Uri): String {
    val documentFile = DocumentFile.fromTreeUri(this, uri)
    val displayName = documentFile?.name

    if (displayName != null) return displayName

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        return cursor.getString(nameIndex)
    }

    return ""
}

fun Context.getAllTasks() {
    val workQuery = WorkQuery.Builder.fromStates(
        listOf(WorkInfo.State.ENQUEUED , WorkInfo.State.RUNNING  , WorkInfo.State.BLOCKED,
               WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, WorkInfo.State.FAILED)
    ).build()
    val workInfoListenableFuture = WorkManager.getInstance(this).getWorkInfos(workQuery)

    workInfoListenableFuture.addListener({
        try {
            val workInfos = workInfoListenableFuture.get()

            for (workInfo in workInfos) {
                println("Task ID: ${workInfo.id}, Status: ${workInfo.state}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, Executors.newSingleThreadExecutor())
}
