package com.simplemobiletools.gallery.pro.ui.adapters

import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.*
import com.simplemobiletools.commons.activities.*
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.views.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.helpers.FOLDER_STYLE_SQUARE
import com.simplemobiletools.gallery.pro.data.models.Directory
import com.simplemobiletools.gallery.pro.data.models.DirectoryGroup

abstract class TasksAdapter(activity: BaseSimpleActivity,
                            recyclerView: MyRecyclerView,
                            fastScroller:FastScroller? = null,
                            itemClick: (Any) -> Unit):
    MyRecyclerViewAdapter(activity, recyclerView, fastScroller, itemClick) {

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//        val layoutType = when {
//            isListViewType -> R.layout.directory_item_list
//            viewType == ITEM_DIRECTORY_GROUP -> R.layout.item_dir_group
//            folderStyle == FOLDER_STYLE_SQUARE -> R.layout.directory_item_grid_square
//            else -> R.layout.directory_item_grid_rounded_corners
//        }
//
//        return createViewHolder(layoutType, parent)
//    }
//
//    override fun onBindViewHolder(holder: MyRecyclerViewAdapter.ViewHolder, position: Int) {
//        val dir = dirs.getOrNull(position) ?: return
//
//        holder.bindView(dir, true, !isPickIntent) { itemView, adapterPosition ->
//            if(dir is Directory || dir is DirectoryGroup)
//                setupView(itemView, dir, holder, position)
//        }
//
//        bindViewHolder(holder)
//    }
//
//    override fun getItemCount() = dirs.size

}
