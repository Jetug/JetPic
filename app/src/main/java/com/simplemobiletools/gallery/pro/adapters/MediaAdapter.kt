package com.simplemobiletools.gallery.pro.adapters

import android.annotation.SuppressLint
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.unipicdev.views.dialogs.DateEditingDialog
import com.simplemobiletools.commons.helpers.SORT_BY_CUSTOM
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_TAKEN
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.MediaActivity
import com.simplemobiletools.gallery.pro.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.extensions.getMediums
import com.simplemobiletools.gallery.pro.extensions.launchIO
import com.simplemobiletools.gallery.pro.helpers.MediaFetcher
import com.simplemobiletools.gallery.pro.interfaces.MediaOperationsListener
import com.simplemobiletools.gallery.pro.jetug.getFolderSorting
import com.simplemobiletools.gallery.pro.jetug.saveCustomMediaOrder
import com.simplemobiletools.gallery.pro.jetug.saveCustomSorting
import com.simplemobiletools.gallery.pro.models.FolderItem
import com.simplemobiletools.gallery.pro.models.ThumbnailItem
import java.util.*

interface MediaAdapterControls{
    fun recreateAdapter()
}

val mediaEmpty = object : MediaAdapterControls{
    override fun recreateAdapter() { }
}

@SuppressLint("ClickableViewAccessibility")
class MediaAdapter(
    private val mediaActivity: SimpleActivity, media: ArrayList<ThumbnailItem>,
    listener: MediaOperationsListener?, isAGetIntent: Boolean,
    allowMultiplePicks: Boolean, path: String, recyclerView: MyRecyclerView,
    fastScroller: FastScroller? = null, swipeRefreshLayout : SwipeRefreshLayout? = null,
    val controls: MediaAdapterControls = mediaEmpty, itemClick: (Any) -> Unit):
    MediaAdapterBase(mediaActivity, media, listener, isAGetIntent, allowMultiplePicks, path, recyclerView,fastScroller, swipeRefreshLayout, itemClick){

    override fun actionItemPressed(id: Int) {
        super.actionItemPressed(id)

        when (id) {
            R.id.editDate -> showDateEditionDialog()
        }
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
            activity.saveCustomSorting(path, SORT_BY_CUSTOM)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sort(){
        val mediaFetcher = MediaFetcher(activity)
        val sorting = activity.getFolderSorting(path)
        mediaFetcher.sortMedia(mediums, sorting)
        notifyDataSetChanged()
    }

    private fun showDateEditionDialog(){
        val paths = getSelectedPaths()
        val dialog = DateEditingDialog(paths) {_,_->
            val sorting = activity.getFolderSorting(path)
            if(sorting and SORT_BY_DATE_TAKEN != 0 || sorting and SORT_BY_DATE_MODIFIED != 0){
                controls.recreateAdapter()
                //mediaActivity.getMedia()
            }
            val s = sorting and SORT_BY_DATE_MODIFIED
            val d = sorting and SORT_BY_DATE_TAKEN
        }
        createDialog(dialog, "")
    }
}
