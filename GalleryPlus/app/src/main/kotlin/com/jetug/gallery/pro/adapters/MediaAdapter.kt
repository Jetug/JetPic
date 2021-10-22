package com.jetug.gallery.pro.adapters

import android.annotation.SuppressLint
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.unipicdev.views.dialogs.DateEditingDialog
import com.jetug.commons.helpers.SORT_BY_DATE_MODIFIED
import com.jetug.commons.helpers.SORT_BY_DATE_TAKEN
import com.jetug.commons.views.FastScroller
import com.jetug.commons.views.MyRecyclerView
import com.jetug.gallery.pro.R
import com.jetug.gallery.pro.activities.MediaActivity
import com.jetug.gallery.pro.interfaces.MediaOperationsListener
import com.jetug.gallery.pro.models.ThumbnailItem
import java.util.ArrayList


@SuppressLint("ClickableViewAccessibility")
class MediaAdapter(
    private val mediaActivity: MediaActivity, media: ArrayList<ThumbnailItem>,
    listener: MediaOperationsListener?, isAGetIntent: Boolean,
    allowMultiplePicks: Boolean, path: String, recyclerView: MyRecyclerView,
    fastScroller: FastScroller? = null, swipeRefreshLayout : SwipeRefreshLayout? = null, itemClick: (Any) -> Unit):
    MediaAdapterBase(mediaActivity, media, listener, isAGetIntent, allowMultiplePicks, path, recyclerView,fastScroller, swipeRefreshLayout, itemClick){

    init {
        //activity.getMedia()
    }

    override fun actionItemPressed(id: Int) {
        super.actionItemPressed(id)

        when (id) {
            R.id.editDate -> showDateEditionDialog()
        }
    }

    private fun showDateEditionDialog(){
        val paths = getSelectedPaths()
        val dialog = DateEditingDialog(paths) {_,_->
            val sorting = config.getFolderSorting(path)
            if(sorting and SORT_BY_DATE_TAKEN != 0 || sorting and SORT_BY_DATE_MODIFIED != 0){
                mediaActivity.getMedia()
            }
            val s = sorting and SORT_BY_DATE_MODIFIED
            val d = sorting and SORT_BY_DATE_TAKEN
        }
        createDialog(dialog, "")
    }
}
