package com.simplemobiletools.gallery.pro.ui.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.extensions.beGoneIf
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.extensions.getTimeFormat
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.commons.views.MyGridLayoutManager
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.ui.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.ui.adapters.SearchResultAdapter
import com.simplemobiletools.gallery.pro.data.helpers.asynctasks.GetMediaAsynctask
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.data.extensions.context.getCachedMedia
import com.simplemobiletools.gallery.pro.data.helpers.SHOW_ALL
import com.simplemobiletools.gallery.pro.data.extensions.context.*
import com.simplemobiletools.gallery.pro.data.models.Medium
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem
import kotlinx.android.synthetic.main.dialog_medium_picker.view.*

class PickMediumDialog(val activity: SimpleActivity, val path: String, val callback: (path: String) -> Unit) {
    var dialog: AlertDialog
    var shownMedia = ArrayList<ThumbnailItem>()
    val view = activity.layoutInflater.inflate(R.layout.dialog_medium_picker, null)
    val viewType = activity.config.getFolderViewType(if (activity.config.showAll) SHOW_ALL else path)
    var isGridViewType = viewType == VIEW_TYPE_GRID

    init {
        (view.media_grid.layoutManager as MyGridLayoutManager).apply {
            orientation = if (activity.config.scrollHorizontally && isGridViewType) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            spanCount = if (isGridViewType) activity.config.mediaColumnCnt else 1
        }

        dialog = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.other_folder) { dialogInterface, i -> showOtherFolder() }
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.select_photo)
            }

        activity.getCachedMedia(path) {
            val media = it.filter { it is Medium } as ArrayList
            if (media.isNotEmpty()) {
                activity.runOnUiThread {
                    gotMedia(media)
                }
            }
        }

        GetMediaAsynctask(activity, path, false, false, false) {
            gotMedia(it)
        }.execute()
    }

    private fun showOtherFolder() {
        PickDirectoryDialog(activity, path, true, true) {
            callback(it)
            dialog.dismiss()
        }
    }

    private fun gotMedia(media: ArrayList<ThumbnailItem>) {
        if (media.hashCode() == shownMedia.hashCode())
            return

        shownMedia = media
        val adapter = SearchResultAdapter(activity, shownMedia.clone() as ArrayList<ThumbnailItem>, null, true, false, path, view.media_grid, null) {
            if (it is Medium) {
                callback(it.path)
                dialog.dismiss()
            }
        }

        val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
        val sorting = activity.getFolderSorting(if (path.isEmpty()) SHOW_ALL else path)
        val dateFormat = activity.config.dateFormat
        val timeFormat = activity.getTimeFormat()
        view.apply {
            media_grid.adapter = adapter

            media_vertical_fastscroller.isHorizontal = false
            media_vertical_fastscroller.beGoneIf(scrollHorizontally)

            media_horizontal_fastscroller.isHorizontal = true
            media_horizontal_fastscroller.beVisibleIf(scrollHorizontally)

            if (scrollHorizontally) {
                media_horizontal_fastscroller.setViews(media_grid) {
                    val medium = (media[it] as? Medium)
                    media_horizontal_fastscroller.updateBubbleText(medium?.getBubbleText(sorting, activity, dateFormat, timeFormat) ?: "")
                }
            } else {
                media_vertical_fastscroller.setViews(media_grid) {
                    val medium = (media[it] as? Medium)
                    media_vertical_fastscroller.updateBubbleText(medium?.getBubbleText(sorting, activity, dateFormat, timeFormat) ?: "")
                }
            }
        }
    }
}
