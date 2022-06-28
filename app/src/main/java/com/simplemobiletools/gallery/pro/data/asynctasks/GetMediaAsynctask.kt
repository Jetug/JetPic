package com.simplemobiletools.gallery.pro.data.asynctasks

import android.content.Context
import android.os.AsyncTask
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.gallery.pro.data.extensions.config
import com.simplemobiletools.gallery.pro.data.extensions.getFavoritePaths
import com.simplemobiletools.gallery.pro.data.helpers.*
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.data.models.Medium
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem
import java.util.*

class GetMediaAsynctask(val context: Context, val mPath: String, val isPickImage: Boolean = false, val isPickVideo: Boolean = false,
                        val showAll: Boolean, val callback: (media: ArrayList<ThumbnailItem>) -> Unit) :
    AsyncTask<Void, Void, ArrayList<ThumbnailItem>>() {
    private val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void): ArrayList<ThumbnailItem> {
        val pathToUse = if (showAll) SHOW_ALL else mPath
        val folderGrouping = context.config.getFolderGrouping(pathToUse)
        val fileSorting = context.getCustomSorting(pathToUse)
        val getProperDateTaken = fileSorting and SORT_BY_DATE_TAKEN != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

        val getProperLastModified = fileSorting and SORT_BY_DATE_MODIFIED != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

        val getProperFileSize = fileSorting and SORT_BY_SIZE != 0
        val favoritePaths = context.getFavoritePaths()
        val getVideoDurations = context.config.showThumbnailVideoDuration
        val lastModifieds = if (getProperLastModified) mediaFetcher.getLastModifieds() else HashMap()
        val dateTakens = if (getProperDateTaken) mediaFetcher.getDateTakens() else HashMap()

        val media = if (showAll) {
            val foldersToScan = mediaFetcher.getFoldersToScan().filter { it != RECYCLE_BIN && it != FAVORITES && !context.config.isFolderProtected(it) }
            val media = ArrayList<Medium>()
            foldersToScan.forEach {
                val newMedia = mediaFetcher.getFilesFrom(it, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified, getProperFileSize,
                    favoritePaths, getVideoDurations, lastModifieds, dateTakens)
                media.addAll(newMedia)
            }

            mediaFetcher.sortMedia(media, context.getCustomSorting(SHOW_ALL))
            media
        } else {
            mediaFetcher.getFilesFrom(mPath, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified, getProperFileSize, favoritePaths,
                getVideoDurations, lastModifieds, dateTakens)
        }

        return mediaFetcher.groupMedia(media, pathToUse)
    }

    override fun onPostExecute(media: ArrayList<ThumbnailItem>) {
        super.onPostExecute(media)
        callback(media)
    }

    fun stopFetching() {
        mediaFetcher.shouldStop = true
        cancel(true)
    }
}


class GetMediaAsynctask2(val context: Context, val mPath: String, val isPickImage: Boolean = false, val isPickVideo: Boolean = false,
                        val showAll: Boolean, val callback: (media: ArrayList<ThumbnailItem>) -> Unit, val show: () -> Unit = {} ) :
        AsyncTask<Void, Void, ArrayList<ThumbnailItem>>() {
    private val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void): ArrayList<ThumbnailItem> {
        val pathToUse = if (showAll) SHOW_ALL else mPath
        val folderGrouping = context.config.getFolderGrouping(pathToUse)
        val fileSorting = context.getCustomSorting(pathToUse)
        val getProperDateTaken = fileSorting and SORT_BY_DATE_TAKEN != 0 ||
                folderGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
                folderGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

        val getProperLastModified = fileSorting and SORT_BY_DATE_MODIFIED != 0 ||
                folderGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
                folderGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

        val getProperFileSize = fileSorting and SORT_BY_SIZE != 0
        val favoritePaths = context.getFavoritePaths()
        val getVideoDurations = context.config.showThumbnailVideoDuration
        val lastModifieds = if (getProperLastModified) mediaFetcher.getLastModifieds() else HashMap()
        val dateTakens = if (getProperDateTaken) mediaFetcher.getDateTakens() else HashMap()

        show()

        val media = if (showAll) {
            val foldersToScan = mediaFetcher.getFoldersToScan().filter { it != RECYCLE_BIN && it != FAVORITES && !context.config.isFolderProtected(it) }
            val media = ArrayList<Medium>()
            foldersToScan.forEach {
                val newMedia = mediaFetcher.getFilesFrom(it, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified, getProperFileSize,
                    favoritePaths, getVideoDurations, lastModifieds, dateTakens)
                media.addAll(newMedia)
            }

            mediaFetcher.sortMedia(media, context.getCustomSorting(SHOW_ALL))
            media
        } else {
            mediaFetcher.getFilesFrom(mPath, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified, getProperFileSize, favoritePaths,
                getVideoDurations, lastModifieds, dateTakens)
        }



        return mediaFetcher.groupMedia(media, pathToUse)
    }

    override fun onPostExecute(media: ArrayList<ThumbnailItem>) {
        super.onPostExecute(media)
        callback(media)
    }

    fun stopFetching() {
        mediaFetcher.shouldStop = true
        cancel(true)
    }
}
