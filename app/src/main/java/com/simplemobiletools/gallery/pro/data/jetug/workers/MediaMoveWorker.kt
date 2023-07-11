package com.simplemobiletools.gallery.pro.data.jetug.workers

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Observer
import java.util.concurrent.TimeUnit

fun Context.createMediaMoveTask(sourcePath: String, destinationPath: String) {
     val inputData = Data.Builder()
         .putString(MediaMoveWorker.KEY_SOURCE_PATH, sourcePath)
         .putString(MediaMoveWorker.KEY_DESTINATION_PATH, destinationPath)
         .build()

     val mediaMoveWorkRequest = PeriodicWorkRequestBuilder<MediaMoveWorker>(15, TimeUnit.MINUTES)
         .setInputData(inputData)
         .build()

     WorkManager.getInstance(this).enqueue(mediaMoveWorkRequest)
}

class MediaMoveWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Retrieve source and destination paths from input data
            val sourcePath = inputData.getString(KEY_SOURCE_PATH)
            val destinationPath = inputData.getString(KEY_DESTINATION_PATH)

            if (sourcePath != null && destinationPath != null) {
                // Move media files from source directory to destination directory
                moveMediaFiles(sourcePath, destinationPath)
            } else {
                throw IllegalArgumentException("Invalid input data")
            }

            Result.success()
        } catch (e: Exception) {
            // Handle any errors
            Result.failure()
        }
    }

    private fun moveMediaFiles(sourcePath: String, destinationPath: String) {
        val sourceDirectory = File(sourcePath)
        val destinationDirectory = File(destinationPath)

        // Loop through files in source directory
        sourceDirectory.listFiles()?.forEach { file ->
            if (file.isFile && file.isMediaFile()) {
                // Move file to destination directory
                file.copyTo(File(destinationDirectory, file.name), true)
                file.delete()
            }
        }
    }

    private fun File.isMediaFile(): Boolean {
        // Add your media file extensions here
        val mediaFileExtensions = arrayOf("png", "jpg", "jpeg", "webp", "svg", "mp4", "gif")
        return mediaFileExtensions.contains(extension.toLowerCase())
    }

    companion object {
        const val KEY_SOURCE_PATH = "source_path"
        const val KEY_DESTINATION_PATH = "destination_path"
    }
}
