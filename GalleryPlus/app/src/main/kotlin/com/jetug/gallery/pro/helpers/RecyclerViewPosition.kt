package com.jetug.gallery.pro.helpers

import androidx.recyclerview.widget.RecyclerView
import com.jetug.commons.views.MyGridLayoutManager
import com.jetug.gallery.pro.extensions.*

class RecyclerViewPosition (val view: RecyclerView){
    private val rvPosition = arrayListOf<Pair<Int,Int>>()
    private val layoutManager get() = view.layoutManager as MyGridLayoutManager

    fun saveRVPosition(){
        val ox = view.computeHorizontalScrollOffset()
        val oy = view.computeVerticalScrollOffset()
        rvPosition.add(Pair(ox, oy))
    }

    fun restoreRVPosition(){
        if(rvPosition.isNotEmpty()) {
            val pos = rvPosition.takeLast()
            layoutManager.scrollToPositionWithOffset(pos.first, -pos.second)
        }
    }

}
