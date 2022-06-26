package com.simplemobiletools.gallery.pro.ui.adapters

import com.simplemobiletools.commons.views.FastScroller
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.data.interfaces.MediaOperationsListener
import com.simplemobiletools.gallery.pro.data.models.ThumbnailItem
import java.util.ArrayList

class SearchResultAdapter(activity: SimpleActivity, media: ArrayList<ThumbnailItem>,
                          listener: MediaOperationsListener?, isAGetIntent: Boolean,
                          allowMultiplePicks: Boolean, path: String, recyclerView: MyRecyclerView,
                          fastScroller: FastScroller? = null, itemClick: (Any) -> Unit):
    MediaAdapterBase(activity, media, listener, isAGetIntent, allowMultiplePicks, path, recyclerView,fastScroller, null, itemClick) {
//    override fun prepareActionMode(menu: Menu) {
//        super.prepareActionMode(menu)
//        menu.apply {
//            //findItem(R.id.cab_change_order).isVisible = false
//        }
//    }
}
