package com.simplemobiletools.gallery.pro.data.jetug.workers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
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
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Retrieve source and destination paths from input data
            val sourcePath = Uri.parse(inputData.getString(KEY_SOURCE_PATH))
            val destinationPath = Uri.parse(inputData.getString(KEY_DESTINATION_PATH))

            if (sourcePath != null && destinationPath != null) {
                context.movePhotosAndVideos(sourcePath, destinationPath)
            } else {
                throw IllegalArgumentException("Invalid input data")
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    fun Context.movePhotosAndVideos(sourceUri: Uri, destinationUri: Uri) {
        val contentResolver = applicationContext.contentResolver
        val sourceDocument = DocumentFile.fromTreeUri(this, sourceUri)
        val destinationDocument = DocumentFile.fromTreeUri(this, destinationUri)

        if (sourceDocument != null && sourceDocument.isDirectory) {
            val files = sourceDocument.listFiles()

            for (file in files) {
                if (!file.isDirectory && isPhotoOrVideoFile(file)) {
                    val sourceFileUri = file.uri
                    val destinationFile = file.name?.let {
                        destinationDocument?.createFile(file.type ?: "application/octet-stream", it)
                    }

                    if (destinationFile != null) {
                        val sourceInputStream = contentResolver.openInputStream(sourceFileUri)
                        val destinationOutputStream = contentResolver.openOutputStream(destinationFile.uri)
                        sourceInputStream?.use { input ->
                            destinationOutputStream?.use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
    }

    fun isPhotoOrVideoFile(file: DocumentFile): Boolean {
        val mimeType = file.type
        return mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true
    }

    companion object {
        const val KEY_SOURCE_PATH = "source_path"
        const val KEY_DESTINATION_PATH = "destination_path"
    }
}
