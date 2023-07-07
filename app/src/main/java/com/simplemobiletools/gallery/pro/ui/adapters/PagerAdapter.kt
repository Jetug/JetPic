package com.simplemobiletools.gallery.pro.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.simplemobiletools.gallery.pro.ui.fragments.TaskCreationFragment
import com.simplemobiletools.gallery.pro.ui.fragments.TaskListFragment

class PagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> TaskCreationFragment()
            1 -> TaskListFragment()
            else -> TaskListFragment()
        }
    }
}
