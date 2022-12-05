package com.simplemobiletools.gallery.pro.ui.adapters

import android.annotation.SuppressLint
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.simplemobiletools.gallery.pro.ui.dialogs.DateEditingDialog
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
import com.simplemobiletools.gallery.pro.data.models.Directory
import com.simplemobiletools.gallery.pro.data.models.DirectoryGroup
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
@SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
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
            R.id.cab_change_order -> changeOrder()
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

    }

    fun sort() {
        val mediaFetcher = MediaFetcher(activity)
        val buffMediums = mediums
        mediaFetcher.sortMedia(buffMediums, activity.getFolderSorting(path))
        media = mediaFetcher.groupMedia(buffMediums, path)
        notifyDataSetChanged()
        updateDirectoryTmb()
    }

    private fun updateDirectoryTmb (){
        if(mediums.isEmpty()) return
        val tmb = mediums.first().path
        var directory = Directory()
        for (dir in mDirs) {
            if(dir is Directory && dir.path == path){
                dir.tmb = tmb
                directory = dir
                break
            }
            else if(dir is DirectoryGroup){
                for (inDir in dir.innerDirs) {
                    if (inDir.path == path) {
                        inDir.tmb = tmb
                        directory = inDir
                        break
                    }
                }
            }
        }
        activity.updateDirectory(directory)
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

            val sorting = activity.getFolderSorting(path)
            if(sorting and SORT_BY_DATE_TAKEN != 0 || sorting and SORT_BY_DATE_MODIFIED != 0){
                sort()
                //controls.recreateAdapter()
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
