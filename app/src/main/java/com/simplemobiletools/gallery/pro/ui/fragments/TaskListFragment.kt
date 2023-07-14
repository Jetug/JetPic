package com.simplemobiletools.gallery.pro.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.data.interfaces.RefreshListener
import com.simplemobiletools.gallery.pro.databinding.FragmentTaskListBinding
import com.simplemobiletools.gallery.pro.ui.adapters.*
import kotlinx.android.synthetic.main.fragment_task_list.view.*

class TaskListFragment : Fragment(), RefreshListener {
    lateinit var binding: FragmentTaskListBinding
    lateinit var root: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentTaskListBinding.inflate(layoutInflater)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_task_list, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createAdapter(view)
    }

    private fun createAdapter(view: View) {
        val activity = requireActivity()
        val tasks = activity.config.tasks.map { it.value }
        view.tasksRV.adapter = TasksAdapter(activity, tasks, this)
        view.tasksRV.layoutManager = LinearLayoutManager(activity)
    }

    override fun refreshItems() {
        root.tasksRV.adapter = null
        createAdapter(root)
    }
}
