package com.simplemobiletools.gallery.pro.ui.adapters

import android.app.Activity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.simplemobiletools.commons.views.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.data.extensions.context.taskDao
import com.simplemobiletools.gallery.pro.data.interfaces.RefreshListener
import com.simplemobiletools.gallery.pro.data.jetug.workers.removeWork
import com.simplemobiletools.gallery.pro.data.models.tasks.AbstractTask
import com.simplemobiletools.gallery.pro.data.models.tasks.SimpleTask
import java.util.*

class TasksAdapter(val activity: Activity, var tasks: List<SimpleTask>,
                   val listener: RefreshListener) : Adapter<TasksAdapter.ViewHolder>() {
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
        fun bindView(item: SimpleTask): View = itemView.apply {
            findViewById<MyTextView>(R.id.taskName).text = item.name
            setOnClickListener { onClick(item, it) }
        }
    }

    private fun onClick(item: SimpleTask, view: View){
        val popupMenu = PopupMenu(activity, view)
        popupMenu.menuInflater.inflate(R.menu.menu_task_item, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.remove -> {
                    activity.taskDao.delete(item)
                    //activity.config.removeTask(item.id)
                    listener.refreshItems()
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }
}
