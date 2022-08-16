package com.simplemobiletools.gallery.pro.ui.adapters

import android.annotation.SuppressLint
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.unipicdev.views.dialogs.DateEditingDialog
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.ui.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.data.helpers.MediaFetcher
import com.simplemobiletools.gallery.pro.data.interfaces.MediaOperationsListener
import com.simplemobiletools.gallery.pro.data.jetug.*
import com.simplemobiletools.gallery.pro.data.models.Medium
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem
import com.simplemobiletools.gallery.pro.ui.activities.mDirs
import com.simplemobiletools.gallery.pro.ui.fragments.MediaFragment.Companion.mMedia
import java.util.*

interface MediaAdapterControls{
    fun recreateAdapter()
}

val mediaEmpty = object : MediaAdapterControls{
    override fun recreateAdapter() { }
}

@SuppressLint("ClickableViewAccessibility")
class MediaAdapter(
    private val mediaActivity: SimpleActivity,
    media: ArrayList<ThumbnailItem>,
    listener: MediaOperationsListener?,
    isAGetIntent: Boolean,
    allowMultiplePicks: Boolean,
    path: String,
    recyclerView: MyRecyclerView,
    fastScroller: FastScroller? = null,
    swipeRefreshLayout : SwipeRefreshLayout? = null,
    val controls: MediaAdapterControls = mediaEmpty, itemClick: (Any) -> Unit):
    MediaAdapterBase(mediaActivity, media, listener, isAGetIntent, allowMultiplePicks, path, recyclerView, fastScroller, swipeRefreshLayout, itemClick){

    override fun actionItemPressed(id: Int) {
        super.actionItemPressed(id)

        when (id) {
            R.id.editDate -> showDateEditionDialog()
            R.id.saveDateToExif -> saveDateToExif()
            //R.id.exifDate -> exifDate()
        }
    }

//    private fun exifDate() {
//        val path = getSelectedPaths()[0]
//        activity.toast(getDateFromExif(path) ?: "")
//    }

    override fun onItemMoved(fromPosition: Int, toPosition: Int){
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(media, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(media, i, i - 1)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onDragAndDroppingEnded(){
        launchIO {
            activity.saveCustomMediaOrder(media.getMediums())
            activity.saveSorting(path, SORT_BY_CUSTOM)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sort() = launchDefault{
        val mediaFetcher = MediaFetcher(activity)
        val sorting = activity.getSorting(path)
        mediaFetcher.sortMedia(mediums, sorting)
        withMainContext { notifyDataSetChanged() }
        //updateDirectory()
    }

    private fun updateDirectory(){
        //Jet
        var dir = mDirs.getDirectories().first { it.path == path }
        dir.tmb = mediums[0].path
        activity.updateDirectory(dir)
    }

    private fun showDateEditionDialog() = launchDefault{
        val paths = getSelectedPaths()
        DateEditingDialog(activity, paths) { dateMap ->
            dateMap.forEach{
                try {
                    val media = mediums.first{ medium ->
                        medium.path == it.key
                    }

                    val item2 = mMedia.first{ medium ->
                        medium is Medium && medium.path == it.key
                    } as Medium

                    media.modified = it.value
                    item2.modified = it.value

                    activity.mediaDB.deleteMedia(media)
                    activity.mediaDB.insert(media)
                }
                catch (ignored: NoSuchElementException){}
            }
            sort()
            val sorting = activity.getSorting(path)
            if(sorting and SORT_BY_DATE_TAKEN != 0 || sorting and SORT_BY_DATE_MODIFIED != 0){
                controls.recreateAdapter()
            }
        }
    }

    private fun saveDateToExif() {
        val paths = getSelectedPaths()
        activity.saveDateToExif(paths, true){
            listener?.refreshItems()
        }
    }
}
