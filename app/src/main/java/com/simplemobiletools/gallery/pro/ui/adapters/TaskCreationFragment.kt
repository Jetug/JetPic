package com.simplemobiletools.gallery.pro.ui.adapters

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.databinding.FragmentTaskListBinding

class TaskCreationFragment : Fragment() {
    // lateinit var binding: Bindin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding = FragmentTaskCreationBinding.inflate(layoutInflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_task_creation, container, false)
    }
}
