package com.simplemobiletools.gallery.pro.data.jetug.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class FileTransferService : Service() {
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sourceUri = Uri.parse(intent?.getStringExtra("sourceUri"))
        val targetUri = Uri.parse(intent?.getStringExtra("targetUri"))

        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                moveMedia(sourceUri, targetUri)
                delay(5000)
            }
        }

        return START_NOT_STICKY
    }

//    private fun moveFiles(sourceUri: String, targetUri: String) {
//        val sourceDirectory = File(Uri.parse(sourceUri).path ?: return)
//        val destinationDirectory = File(Uri.parse(targetUri).path ?: return)
//
//        val sourceFiles = sourceDirectory.listFiles() ?: return
//        for (file in sourceFiles) {
//            if (isPhotoOrVideoFile(file)) {
//                val destinationFile = File(destinationDirectory, file.name)
//                file.renameTo(destinationFile)
//            }
//        }
//    }
//
//    private fun isPhotoOrVideoFile(file: File): Boolean {
//        val extension = file.extension.toLowerCase()
//        return extension == "jpg" || extension == "jpeg" || extension == "png" ||
//            extension == "gif" || extension == "mp4" || extension == "mov"
//    }

    private fun Context.moveMedia(sourceUri: Uri, destinationUri: Uri) {
        val contentResolver = applicationContext.contentResolver
        val sourceDocument = DocumentFile.fromTreeUri(this, sourceUri)
        val destinationDocument = DocumentFile.fromTreeUri(this, destinationUri)

        if (sourceDocument != null) {
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}
