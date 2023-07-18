package com.simplemobiletools.gallery.pro.data.jetug.workers

import android.content.*
import android.net.*
import android.provider.Settings.*
import androidx.work.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.data.jetug.*
import com.simplemobiletools.gallery.pro.data.jetug.services.FileTransferService
import com.simplemobiletools.gallery.pro.data.models.tasks.*
import java.io.File
import java.util.*
import java.util.concurrent.*

fun Context.createMediaMoveTask(sourcePath: Uri, destinationPath: Uri) {
    mediaMoveService(sourcePath, destinationPath)
}

fun BaseSimpleActivity.mediaMoveService(sourcePath: String, destinationPath: String) {
    val intent = Intent(this, FileTransferService::class.java)
    intent.putExtra("sourceUri", sourcePath.toString())
    intent.putExtra("targetUri", destinationPath.toString())
    startService(intent)

    val id = "mediaMoveWorkRequest.id"
    val name = File(sourcePath).name + " to " + File(destinationPath).name
    config.addTask(SimpleTask(id, name))
}

fun Context.mediaMoveService(sourcePath: Uri, destinationPath: Uri) {
    val intent = Intent(this, FileTransferService::class.java)
    intent.putExtra("sourceUri", sourcePath.toString())
    intent.putExtra("targetUri", destinationPath.toString())
    startService(intent)

    val id = "mediaMoveWorkRequest.id"
    val name = getDirectoryNameFromUri(sourcePath) + " to " + getDirectoryNameFromUri(destinationPath)
    config.addTask(SimpleTask(id.toString(), name))
}

fun Context.mediaMoveWorker(sourcePath: Uri, destinationPath: Uri) {
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
    requestIgnoreBatteryOptimizations()
    WorkManager.getInstance(this).enqueue(mediaMoveWorkRequest)
}

fun Context.requestIgnoreBatteryOptimizations() {
    val packageName = applicationContext.packageName
    val intent = Intent().apply {
        action = ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.parse("package:$packageName")
    }
    startActivity(intent)
}

fun Context.removeWork(taskId: UUID) {
    val workManager = WorkManager.getInstance(applicationContext)
    workManager.cancelWorkById(taskId)
}

fun Context.getAllTasks(): MutableList<WorkInfo>? {
    val workQuery = WorkQuery.Builder.fromStates(
        listOf(WorkInfo.State.ENQUEUED , WorkInfo.State.RUNNING  , WorkInfo.State.BLOCKED,
               WorkInfo.State.SUCCEEDED, WorkInfo.State.CANCELLED, WorkInfo.State.FAILED))
        .build()

    val workInfoListenableFuture = WorkManager.getInstance(this).getWorkInfos(workQuery)
    return workInfoListenableFuture.get()
}
