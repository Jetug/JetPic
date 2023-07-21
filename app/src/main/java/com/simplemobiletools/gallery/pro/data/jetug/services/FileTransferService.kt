package com.simplemobiletools.gallery.pro.data.jetug.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.context.taskDao
import com.simplemobiletools.gallery.pro.data.extensions.*
import java.io.File

private const val PERIOD = 10 * 1000L

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
            try {
                for (it in taskDao.getAll())
                    moveFiles(it.sourcePath, it.targetPath)
                handler.postDelayed(runnable, PERIOD)
            }
            catch (e: Exception){e.printStackTrace()}
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        handler.postDelayed(runnable, PERIOD)
        return START_STICKY
    }

    private fun moveFiles(sourceUri: String, targetUri: String) {
        val sourceDir = File(sourceUri)

        if (sourceDir.exists() && sourceDir.isDirectory) {
            val files = sourceDir.listFiles { file -> file.isPhotoVideo }
            files?.forEach { file ->
                file.move(targetUri)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    private fun createNotification(): Notification {
        val channelId = "tasks"
        val channelName = resources.getString(R.string.tasks)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Redirection Service")
            .setContentText("File moving service is running.")
            .setSmallIcon(R.drawable.ic_folder_vector)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(channelId)
        }

        return builder.build()
    }
}
