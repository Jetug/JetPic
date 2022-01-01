package com.jetug.gallery.pro.activities.contracts

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.jetug.gallery.pro.PickDirectoryActivity

class PickDirectoryContract : ActivityResultContract<String, String?>() {

    override fun createIntent(context: Context, input: String?): Intent {
        return Intent(context, PickDirectoryActivity::class.java)
            .putExtra("sourcePath", input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? = when {
        resultCode != Activity.RESULT_OK -> null
        else -> intent?.getStringExtra("resultPath")
    }

    override fun getSynchronousResult(context: Context, input: String?): SynchronousResult<String?>? {
        return if (input.isNullOrEmpty()) SynchronousResult("") else null
    }
}
