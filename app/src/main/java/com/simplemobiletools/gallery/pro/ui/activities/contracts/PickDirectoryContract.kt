package com.simplemobiletools.gallery.pro.ui.activities.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.simplemobiletools.gallery.pro.ui.activities.*
import com.simplemobiletools.gallery.pro.data.helpers.PICK_DIR_INPUT_PATH
import com.simplemobiletools.gallery.pro.data.helpers.PICK_DIR_OUTPUT_PATH

class PickDirectoryContract : ActivityResultContract<String, String?>() {
    override fun createIntent(context: Context, input: String?): Intent {
        return Intent(context, PickDirectoryActivity::class.java)
            .putExtra(PICK_DIR_INPUT_PATH, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? = when {
        resultCode != Activity.RESULT_OK -> null
        else -> intent?.getStringExtra(PICK_DIR_OUTPUT_PATH)
    }

    override fun getSynchronousResult(context: Context, input: String?): SynchronousResult<String?>? {
        return if (input.isNullOrEmpty()) SynchronousResult("") else null
    }
}
