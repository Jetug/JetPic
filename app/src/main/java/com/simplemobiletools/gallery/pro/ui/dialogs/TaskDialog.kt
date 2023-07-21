package com.simplemobiletools.gallery.pro.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.google.android.flexbox.FlexboxLayout
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.internalStoragePath
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyEditText
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.context.config
import com.simplemobiletools.gallery.pro.data.interfaces.ResultListener
import com.simplemobiletools.gallery.pro.data.jetug.workers.*
//import droidninja.filepicker.FilePickerBuilder
//import droidninja.filepicker.utils.ContentUriUtils
import kotlinx.android.synthetic.main.dialog_task.view.*


class TaskDialog(val activity: BaseSimpleActivity, val onComplete: () -> Unit = {}) {
    private val view: View = activity.layoutInflater.inflate(R.layout.dialog_task, null)
    private val dialog: AlertDialog

    init {
        //if(activity is ResultListener) activity.onResult = ::onActivityResult
        dialog = AlertDialog.Builder(activity)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.new_task) {
                    onCreate()
                }
            }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    private fun onCreate(){
          view.apply {
              addBtn.setOnClickListener {
                  val textView = createTextView()
                  flowLayout.addView(textView, layoutParams)
              }

              selectSourcePath.setOnClickListener {
                  FilePickerDialog(activity, activity.internalStoragePath, false,
                      activity.config.shouldShowHidden, false, true){
                      sourceUri = it
                      view.sourcePath.setText(it)
                  }
              }

              selectPath.setOnClickListener {
                  FilePickerDialog(activity, activity.internalStoragePath, false,
                      activity.config.shouldShowHidden, false, true){
                      destinationUri = it
                      view.path.setText(it)

                  }
              }
              //okBtn.isEnabled = false
              okBtn.setOnClickListener(::onPositiveButtonClick)
          }
      }

    private fun createTextView(): MyEditText {
        val textView = MyEditText(activity).apply {
          layoutParams = FlexboxLayout.LayoutParams(80,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return textView
    }

    private var sourceUri: String = ""
    private var destinationUri: String = ""

//    private fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
//            sourceUri = getDirectoryPathFromUri(data?.data!!)
//            view.sourcePath.setText(sourceUri)
//        }
//        else if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
//            destinationUri = getDirectoryPathFromUri(data?.data!!)
//            view.path.setText(destinationUri)
//        }
//    }

    private fun getDirectoryPathFromUri(uri: Uri): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volumePath = DocumentsContract.getTreeDocumentId(uri).split(":")[1]
            return "${Environment.getExternalStorageDirectory().path}/$volumePath"
        } else {
            val pathSplitted = uri.path?.split(':')
            if (pathSplitted != null && pathSplitted.size > 1) {
                val volumePath = pathSplitted[1]
                return "${Environment.getExternalStorageDirectory().path}/$volumePath"
            }
        }
        return ""
    }

    private fun onPositiveButtonClick(v: View){
        if (sourceUri == destinationUri) return
        activity.newRedirectTask(sourceUri, destinationUri)
        onComplete()
        dialog.cancel()

    }
}
