  package com.simplemobiletools.gallery.pro.ui.dialogs

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.children
import com.google.android.flexbox.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.views.MyEditText
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.TasksActivity
import com.simplemobiletools.gallery.pro.data.extensions.launchIO
import com.simplemobiletools.gallery.pro.data.extensions.launchMain
import com.simplemobiletools.gallery.pro.data.helpers.JET
import com.simplemobiletools.gallery.pro.data.interfaces.ResultListener
import com.simplemobiletools.gallery.pro.ui.fragments.TaskCreationFragment
import kotlinx.android.synthetic.main.dialog_date_editing.view.*
import kotlinx.android.synthetic.main.dialog_task.view.*
import org.apmem.tools.layouts.FlowLayout
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.Period
import java.io.File
import kotlin.system.measureTimeMillis

class TaskDialog(val activity: BaseSimpleActivity, val onComplete: () -> Unit = {}) {

    private val view: View = activity.layoutInflater.inflate(R.layout.dialog_task, null)

    init {
        if(activity is ResultListener) activity.onResult = ::onActivityResult
        AlertDialog.Builder(activity)
//            .setPositiveButton(R.string.ok, ::onPositiveButtonClick)
//            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.edit_date) {
                    onCreate()
                }
            }
    }

    private fun onCreate(){
        view.apply {
            addBtn.setOnClickListener {
                val textView = createTextView()
                flowLayout.addView(textView, layoutParams)
            }

            selectSourcePath.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                activity.startActivityForResult(intent, 0)
            }

            selectPath.setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                activity.startActivityForResult(intent, 1)
            }
           // findViewById<>(R.id.selectPath)
        }
    }


    private fun createTextView(): MyEditText {
        val textView = MyEditText(activity).apply {
            layoutParams = FlexboxLayout.LayoutParams(80,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return textView
    }


    var sourceUri: Uri? = null
    var destinationUri: Uri? = null

    private fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            sourceUri = data?.data
            view.sourcePath.setText(sourceUri?.path)
        }
        else if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            destinationUri = data?.data
            view.path.setText(destinationUri?.path)
        }

    }

    private fun onPositiveButtonClick(dialog: DialogInterface, id: Int) = launchIO{

    }
}
