package com.simplemobiletools.gallery.pro.data.interfaces

import android.content.Intent

interface ResultListener {
    var onResult: (Int, Int, Intent?) -> Unit
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}
