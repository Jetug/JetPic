package com.simplemobiletools.gallery.pro.data.jetug.workers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
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



    WorkManager.getInstance(this).enqueue(mediaMoveWorkRequest)
}

fun Context.getFileNameFromUri(uri: Uri): String? {
    val document = DocumentFile.fromSingleUri(applicationContext, uri)

    // Check if the document exists and is not a directory
    if (document != null && !document.isDirectory) {
        return document.name
    }

    return null
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
