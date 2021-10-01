package com.jetug.gallery.pro.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jetug.commons.helpers.REFRESH_PATH
import com.jetug.gallery.pro.extensions.addPathToDB

class RefreshMediaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra(REFRESH_PATH) ?: return
        context.addPathToDB(path)
    }
}
