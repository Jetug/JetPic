package com.simplemobiletools.gallery.pro.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.interfaces.RefreshListener
import com.simplemobiletools.gallery.pro.databinding.*
import com.simplemobiletools.gallery.pro.databinding.*
import com.simplemobiletools.gallery.pro.ui.dialogs.*
import kotlinx.android.synthetic.main.fragment_task_creation.view.*

class TaskCreationFragment : Fragment() {
    lateinit var binding: FragmentTaskCreationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentTaskCreationBinding.inflate(layoutInflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_task_creation, container, false)

        root.apply {
            newTaskBtn.setOnClickListener {
                val activity = activity

                TaskDialog(activity as BaseSimpleActivity){
                    if(activity is RefreshListener) activity.refreshItems()
                }
            }
        }

        return root
    }
}
