package com.simplemobiletools.gallery.pro

import androidx.appcompat.app.*
import android.os.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.*
import com.google.android.material.tabs.*
import com.google.android.material.tabs.*
import com.simplemobiletools.commons.views.*
import com.simplemobiletools.gallery.pro.databinding.*
import com.simplemobiletools.gallery.pro.ui.adapters.*

class TasksActivity : AppCompatActivity() {
    lateinit var binding: ActivityTasksBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTasksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        val viewPager: ViewPager2 = findViewById(R.id.view_pager)

        val adapter = PagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Tab A"
                1 -> tab.text = "Tab B"
            }
        }.attach()
    }


}
