package com.simplemobiletools.gallery.pro.ui.adapters

import android.annotation.SuppressLint
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.unipicdev.views.dialogs.DateEditingDialog
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.SORT_BY_CUSTOM
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_TAKEN
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.ui.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.data.extensions.*
import com.simplemobiletools.gallery.pro.data.helpers.MediaFetcher
import com.simplemobiletools.gallery.pro.data.interfaces.MediaOperationsListener
import com.simplemobiletools.gallery.pro.data.jetug.*
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem
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
            R.id.exifDate -> exifDate()
        }
    }

    private fun exifDate() {
        val path = getSelectedPaths()[0]
        activity.toast(getDateFromExif(path) ?: "")
    }

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
    fun sort(){
        val mediaFetcher = MediaFetcher(activity)
        val sorting = activity.getSorting(path)
        mediaFetcher.sortMedia(mediums, sorting)
        notifyDataSetChanged()
    }

    private fun showDateEditionDialog(){
        val paths = getSelectedPaths()
        DateEditingDialog(activity, paths) {_,_->
            val sorting = activity.getSorting(path)
            if(sorting and SORT_BY_DATE_TAKEN != 0 || sorting and SORT_BY_DATE_MODIFIED != 0){
                controls.recreateAdapter()
            }
        }
    }

    private fun saveDateToExif() {
        val paths = getSelectedPaths()
        activity.saveDateTakenToExif(paths, true){
            listener?.refreshItems()
        }
    }
}
