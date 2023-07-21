package com.simplemobiletools.gallery.pro.data.jetug.workers

import android.app.ActivityManager
import android.content.*
import android.net.*
import android.os.Build.*
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings.*
import androidx.work.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.helpers.isOreoPlus
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.data.jetug.services.*
import com.simplemobiletools.gallery.pro.data.models.tasks.*
import java.io.File

val Context.hasTasks get() = taskDao.getAll().isNotEmpty()
val Context.isRedirectServiceRunning get() = isServiceRunning(FileTransferService::class.java)

fun Context.newRedirectTask(sourcePath: String, destinationPath: String) {
    val name = File(sourcePath).name + " to " + File(destinationPath).name
    taskDao.insert(SimpleTask(0, name, sourcePath, destinationPath))

    startRedirectService()
//    if (isOreoPlus() && !isServiceRunning(FileTransferService::class.java))
//        startForegroundService(Intent(this, FileTransferService::class.java))
}

fun Context.startRedirectService(){
    if(hasTasks && isOreoPlus() && !isRedirectServiceRunning)
        startForegroundService(Intent(this, FileTransferService::class.java))
}

fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    for (service in manager.getRunningServices(Integer.MAX_VALUE))
        if (serviceClass.name == service.service.className) return true
    return false
}

fun Context.requestIgnoreBatteryOptimizations() {
    val packageName = applicationContext.packageName
    val intent = Intent().apply {
        action = ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        data = Uri.parse("package:$packageName")
    }
    startActivity(intent)
}
