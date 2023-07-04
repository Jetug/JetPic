package com.simplemobiletools.gallery.pro.ui.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.*
import com.simplemobiletools.commons.activities.*
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.views.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.models.Directory
import com.simplemobiletools.gallery.pro.data.models.DirectoryGroup
import com.simplemobiletools.gallery.pro.data.models.tasks.AbstractTask

class TasksAdapter(var tasks: ArrayList<AbstractTask>, itemClick: (Any) -> Unit) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //holder.itemTextView.text = tasks[position]
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }
}
