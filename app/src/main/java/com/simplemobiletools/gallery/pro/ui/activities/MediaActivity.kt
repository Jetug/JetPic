package com.simplemobiletools.gallery.pro.ui.activities

import android.os.Bundle
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.ui.fragments.MediaFragment
import com.simplemobiletools.gallery.pro.data.helpers.DIRECTORY

class MediaActivity : SimpleActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media)
        initFragment()
    }

    private fun initFragment(){
        val fragment = MediaFragment()
        val bundle = Bundle()
        bundle.putString(DIRECTORY, intent.getStringExtra(DIRECTORY))
        //bundle.putBoolean(SKIP_AUTHENTICATION, intent.getBooleanExtra(SKIP_AUTHENTICATION, false))
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.mainContent, fragment)
            .commit()
    }
}
