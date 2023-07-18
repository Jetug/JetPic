package com.simplemobiletools.gallery.pro.data.jetug.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.data.extensions.context.taskDao
import com.simplemobiletools.gallery.pro.data.extensions.launchIO
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

    override fun onCreate() {
        super.onCreate()

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            moveFiles() // Call the method to move files
            handler.postDelayed(runnable, 5000) // Repeat every 5 seconds (5000 milliseconds)
        }
    }

//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        val sourceUri = intent?.getStringExtra("sourceUri")
//        val targetUri = intent?.getStringExtra("targetUri")
//        if(sourceUri == null || targetUri == null) return START_STICKY
//
//        GlobalScope.launch(Dispatchers.IO) {
//            while (true) {
//                moveMedia(sourceUri, targetUri)
//                delay(5000)
//            }
//        }
//
//        return START_STICKY
//    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
//        launchIO { while (true) {
//            moveFiles()
//            delay(5000)
//        } }
        startForeground(1, createNotification())
        handler.postDelayed(runnable, 5000)
        return START_STICKY
    }

    private fun moveFiles(){
        taskDao.getAll().forEach{
            moveFiles(it.sourcePath, it.targetPath)
        }
    }

    private fun moveFiles(sourceUri: String, targetUri: String) {
        val sourceDir = File(sourceUri)
        val destinationDir = File(targetUri)

        if (sourceDir.exists() && sourceDir.isDirectory) {
            val files = sourceDir.listFiles { file ->
                // Return true if you want to include the file in the move operation
                file.extension == "jpg" || file.extension == "jpeg" ||
                    file.extension == "png" || file.extension == "mp4"
            }
            files?.forEach { file ->
                // Move file to the destination directory
                val destFile = File(destinationDir, file.name)
                file.renameTo(destFile)
            }
        }
    }
//
//    private fun isPhotoOrVideoFile(file: File): Boolean {
//        val extension = file.extension.toLowerCase()
//        return extension == "jpg" || extension == "jpeg" || extension == "png" ||
//            extension == "gif" || extension == "mp4" || extension == "mov"
//    }

    private fun Context.moveMedia(source: String, destination: String) {
        val sourceFile = File(source)
        val destinationDirectory = File(destination)

        if (sourceFile.exists() && destinationDirectory.isDirectory) {
            val newFile = File(destinationDirectory, sourceFile.name)
            val isMoved = sourceFile.renameTo(newFile)

            if (isMoved) {
                println("1")
                // File moved successfully
            } else {
                println("0")
                // File move failed
            }
        } else {
            // Source file doesn't exist or destination directory is invalid
            // Handle the appropriate error case
        }
    }

    private fun DocumentFile.isPhotoVideo(): Boolean =
        type?.startsWith("image/") == true ||
        type?.startsWith("video/") == true

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun createNotification(): Notification {
        val channelId = "your_channel_id"
        val channelName = "Your Channel Name"
        val notificationId = 1

        // Build a notification using NotificationCompat.Builder
        // Set appropriate values for title, content, and other notification properties
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("File Moving Service")
            .setContentText("File moving service is running.")
            .setSmallIcon(R.drawable.ic_file_ai)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Create a notification channel if necessary (required on API level 26 and higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }

        return builder.build()
    }
}
