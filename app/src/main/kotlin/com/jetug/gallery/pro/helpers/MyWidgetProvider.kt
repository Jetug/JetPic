package com.jetug.gallery.pro.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.jetug.commons.extensions.getFileSignature
import com.jetug.commons.extensions.setBackgroundColor
import com.jetug.commons.extensions.setText
import com.jetug.commons.extensions.setVisibleIf
import com.jetug.commons.helpers.ensureBackgroundThread
import com.jetug.gallery.pro.R
import com.jetug.gallery.pro.activities.MediaActivity
import com.jetug.gallery.pro.extensions.config
import com.jetug.gallery.pro.extensions.directoryDao
import com.jetug.gallery.pro.extensions.getFolderNameFromPath
import com.jetug.gallery.pro.extensions.widgetsDB
import com.jetug.gallery.pro.models.Widget

class MyWidgetProvider : AppWidgetProvider() {
    private fun setupAppOpenIntent(context: Context, views: RemoteViews, id: Int, widget: Widget) {
        val intent = Intent(context, MediaActivity::class.java).apply {
            putExtra(DIRECTORY, widget.folderPath)
        }

        val pendingIntent = PendingIntent.getActivity(context, widget.widgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(id, pendingIntent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ensureBackgroundThread {
            val config = context.config
            context.widgetsDB.getWidgets().filter { appWidgetIds.contains(it.widgetId) }.forEach {
                val views = RemoteViews(context.packageName, R.layout.widget).apply {
                    setBackgroundColor(R.id.widget_holder, config.widgetBgColor)
                    setVisibleIf(R.id.widget_folder_name, config.showWidgetFolderName)
                    setTextColor(R.id.widget_folder_name, config.widgetTextColor)
                    setText(R.id.widget_folder_name, context.getFolderNameFromPath(it.folderPath))
                }

                val path = context.directoryDao.getDirectoryThumbnail(it.folderPath) ?: return@forEach
                val options = RequestOptions()
                        .signature(path.getFileSignature())
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)

                if (context.config.cropThumbnails) {
                    options.centerCrop()
                } else {
                    options.fitCenter()
                }

                val density = context.resources.displayMetrics.density
                val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetIds.first())
                val width = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val height = appWidgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)

                val widgetSize = (Math.max(width, height) * density).toInt()
                try {
                    val image = Glide.with(context)
                            .asBitmap()
                            .load(path)
                            .apply(options)
                            .submit(widgetSize, widgetSize)
                            .get()
                    views.setImageViewBitmap(R.id.widget_imageview, image)
                } catch (e: Exception) {
                }

                setupAppOpenIntent(context, views, R.id.widget_holder, it)

                try {
                    appWidgetManager.updateAppWidget(it.widgetId, views)
                } catch (ignored: Exception) {
                }
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        ensureBackgroundThread {
            appWidgetIds.forEach {
                context.widgetsDB.deleteWidgetId(it)
            }
        }
    }
}
