package com.simplemobiletools.gallery.pro.ui.adapters

import android.annotation.SuppressLint
import android.view.Menu
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_TAKEN
import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.context.getFolderSorting
import com.simplemobiletools.gallery.pro.data.extensions.context.mediaDB
import com.simplemobiletools.gallery.pro.data.extensions.context.updateDirectory
import com.simplemobiletools.gallery.pro.data.extensions.launchDefault
import com.simplemobiletools.gallery.pro.data.extensions.launchMain
import com.simplemobiletools.gallery.pro.data.helpers.MediaFetcher
import com.simplemobiletools.gallery.pro.data.interfaces.MediaAdapterControls
import com.simplemobiletools.gallery.pro.data.jetug.alignDate
import com.simplemobiletools.gallery.pro.data.jetug.saveDateToExif
import com.simplemobiletools.gallery.pro.data.models.Directory
import com.simplemobiletools.gallery.pro.data.models.DirectoryGroup
import com.simplemobiletools.gallery.pro.data.models.Medium
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem
import com.simplemobiletools.gallery.pro.ui.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.ui.activities.mDirs
import com.simplemobiletools.gallery.pro.ui.dialogs.DateEditingDialog
import com.simplemobiletools.gallery.pro.ui.fragments.MediaFragment.Companion.mMedia
import java.util.*

@SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
class MediaAdapter(
    mediaActivity: SimpleActivity,
    media: ArrayList<ThumbnailItem>,
    listener: MediaAdapterControls?,
    isAGetIntent: Boolean,
    allowMultiplePicks: Boolean,
    path: String,
    recyclerView: MyRecyclerView,
    fastScroller: FastScroller? = null,
    swipeRefreshLayout : SwipeRefreshLayout? = null,
    itemClick: (Any) -> Unit):
    MediaAdapterBase(mediaActivity, media, listener, isAGetIntent, allowMultiplePicks, path, recyclerView, fastScroller, swipeRefreshLayout, itemClick){

    override fun prepareActionMode(menu: Menu) {
        super.prepareActionMode(menu)
        menu.apply {
            findItem(R.id.align_date).isVisible = true
        }
    }

    override fun actionItemPressed(id: Int) {
        super.actionItemPressed(id)

        when (id) {
            R.id.editDate -> showDateEditionDialog()
            R.id.saveDateToExif -> saveDateToExif()
            R.id.align_date -> alignDate()
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

    private fun saveDateToExif() {
        mediums
        activity.saveDateToExif(selectedPaths, true){
            listener?.refreshItems()
        }
    }

    private fun alignDate() {
        activity.alignDate(selectedItems){

        }
    }

    private fun showDateEditionDialog() = launchDefault{
        val paths = selectedPaths
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
                launchMain { sort() }
                //controls.recreateAdapter()
            }
        }
    }
}
