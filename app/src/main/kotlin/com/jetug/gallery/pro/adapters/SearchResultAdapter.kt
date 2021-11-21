package com.jetug.gallery.pro.adapters

import android.view.Menu
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.views.FastScroller
import com.jetug.commons.views.MyRecyclerView
import com.jetug.gallery.pro.R
import com.jetug.gallery.pro.interfaces.MediaOperationsListener
import com.jetug.gallery.pro.models.ThumbnailItem
import java.util.ArrayList

class SearchResultAdapter(activity: BaseSimpleActivity, media: ArrayList<ThumbnailItem>,
                          listener: MediaOperationsListener?, isAGetIntent: Boolean,
                          allowMultiplePicks: Boolean, path: String, recyclerView: MyRecyclerView,
                          fastScroller: FastScroller? = null, itemClick: (Any) -> Unit):
    MediaAdapterBase(activity, media, listener, isAGetIntent, allowMultiplePicks, path, recyclerView,fastScroller, null, itemClick) {
    override fun prepareActionMode(menu: Menu) {
        super.prepareActionMode(menu)
        menu.apply {
            findItem(R.id.cab_change_order).isVisible = false
        }
    }
}
