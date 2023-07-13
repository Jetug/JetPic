package com.simplemobiletools.gallery.pro.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.ui.adapters.TasksAdapter

class AdapterTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adapter_test)
        createAdapter()
    }

    private fun createAdapter(){
        val recyclerView = findViewById<RecyclerView>(R.id.mainRV)
        val d = this.config.tasks.map { it.value }
        recyclerView.adapter = TasksAdapter(this, d!!){}
        recyclerView.layoutManager = LinearLayoutManager(this)
    }
}
