package com.simplemobiletools.gallery.pro.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.databinding.FragmentTaskListBinding
import com.simplemobiletools.gallery.pro.ui.adapters.TasksAdapter

class TaskListFragment : Fragment() {
    lateinit var binding: FragmentTaskListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentTaskListBinding.inflate(layoutInflater)
        createAdapter()

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    private fun createAdapter(){
        val recyclerView = binding.recyclerView
        recyclerView.adapter = TasksAdapter(arrayListOf()){}
        recyclerView.layoutManager = LinearLayoutManager(this.context)
    }
}
