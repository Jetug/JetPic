package com.simplemobiletools.gallery.pro.ui.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.*
import com.simplemobiletools.commons.activities.*
import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter
import com.simplemobiletools.commons.views.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.models.Directory
import com.simplemobiletools.gallery.pro.data.models.DirectoryGroup
import com.simplemobiletools.gallery.pro.data.models.tasks.AbstractTask
import com.simplemobiletools.gallery.pro.data.models.tasks.SimpleTask
import kotlinx.android.synthetic.main.item_task.view.*

class TasksAdapter(val activity: Activity, var tasks: List<SimpleTask>, val itemClick: (Any) -> Unit) : RecyclerView.Adapter<TasksAdapter.ViewHolder>() {

    init {
        val t = tasks
        println(t)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(activity.layoutInflater.inflate(R.layout.item_task, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(tasks[position])
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(item: SimpleTask): View {
            return itemView.apply {
                findViewById<MyTextView>(R.id.taskName).text = item.name
                setOnClickListener { itemClick(item) }
            }
        }
    }
}
