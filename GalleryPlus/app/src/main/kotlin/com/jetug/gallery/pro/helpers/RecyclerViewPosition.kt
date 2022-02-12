package com.jetug.gallery.pro.helpers

import androidx.recyclerview.widget.RecyclerView
import com.jetug.commons.views.MyGridLayoutManager
import com.jetug.gallery.pro.extensions.*

class RecyclerViewPosition (val view: RecyclerView?){
    private val rvPosition = arrayListOf<Pair<Int,Int>>()

    fun saveRVPosition(){
        if(view != null) {
            val ox = view.computeHorizontalScrollOffset()
            val oy = view.computeVerticalScrollOffset()
            rvPosition.add(Pair(ox, oy))
        }
    }

    fun restoreRVPosition(){
        if(rvPosition.isNotEmpty() && view != null) {
            val pos = rvPosition.takeLast()
            val layoutManager = view.layoutManager as MyGridLayoutManager
            layoutManager.scrollToPositionWithOffset(pos.first, -pos.second)
        }
    }

}
