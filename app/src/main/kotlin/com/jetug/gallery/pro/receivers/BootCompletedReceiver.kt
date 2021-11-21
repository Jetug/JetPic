package com.jetug.gallery.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jetug.commons.helpers.ensureBackgroundThread
import com.jetug.gallery.pro.extensions.updateDirectoryPath
import com.jetug.gallery.pro.helpers.MediaFetcher

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            MediaFetcher(context).getFoldersToScan().forEach {
                context.updateDirectoryPath(it)
            }
        }
    }
}
