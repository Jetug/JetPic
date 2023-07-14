package com.simplemobiletools.gallery.pro.data.jetug.workers

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val KEY_SOURCE_PATH = "source_path"
const val KEY_DESTINATION_PATH = "destination_path"

class MediaMoveWorker(
    val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sourcePath = Uri.parse(inputData.getString(KEY_SOURCE_PATH))
            val destinationPath = Uri.parse(inputData.getString(KEY_DESTINATION_PATH))

            if (sourcePath != null && destinationPath != null) {
                context.moveMedia(sourcePath, destinationPath)
            } else {
                throw IllegalArgumentException("Invalid input data")
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun Context.moveMedia(sourceUri: Uri, destinationUri: Uri) {
        val contentResolver = applicationContext.contentResolver
        val sourceDocument = DocumentFile.fromTreeUri(this, sourceUri)
        val destinationDocument = DocumentFile.fromTreeUri(this, destinationUri)

        if (sourceDocument != null && sourceDocument.isDirectory) {
            val files = sourceDocument.listFiles()

            for (file in files) {
                if (!file.isDirectory && file.isPhotoVideo()) {
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
                                file.delete()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DocumentFile.isPhotoVideo(): Boolean =
        type?.startsWith("image/") == true ||
        type?.startsWith("video/") == true
}
