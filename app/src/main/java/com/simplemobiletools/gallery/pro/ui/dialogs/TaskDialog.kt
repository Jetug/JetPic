package com.simplemobiletools.gallery.pro.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import com.google.android.flexbox.FlexboxLayout
import com.leon.lfilepickerlibrary.LFilePicker
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.views.MyEditText
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.copyFile
import com.simplemobiletools.gallery.pro.data.interfaces.ResultListener
import com.simplemobiletools.gallery.pro.data.jetug.workers.*
//import droidninja.filepicker.FilePickerBuilder
//import droidninja.filepicker.utils.ContentUriUtils
import kotlinx.android.synthetic.main.dialog_task.view.*
import me.rosuh.filepicker.config.FilePickerManager
import java.io.File


class TaskDialog(val activity: BaseSimpleActivity, val onComplete: () -> Unit = {}) {
    private val view: View = activity.layoutInflater.inflate(R.layout.dialog_task, null)
    private val dialog: AlertDialog

    init {
        if(activity is ResultListener) activity.onResult = ::onActivityResult
        dialog = AlertDialog.Builder(activity)
//            .setPositiveButton(R.string.ok, ::onPositiveButtonClick)
//            .setNegativeButton(R.string.cancel, null)
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

//                  LFilePicker()
//                      .withActivity(activity)
//                      .withRequestCode(0)
//                      .withChooseMode(false)
//                      .withIsGreater(false)
//                      .start()
//
//                  FilePickerManager
//                      .from(activity)
//                      .forResult(FilePickerManager.REQUEST_CODE)

                  val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                  activity.startActivityForResult(intent, 0)
              }

              selectPath.setOnClickListener {
                  val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                  activity.startActivityForResult(intent, 1)
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

    private var sourceUri: Uri? = null
    private var destinationUri: Uri? = null

    private fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            sourceUri = data?.data
            if(data == null) return
//            val path: String? = data.getStringExtra("path")
//            view.sourcePath.setText(path)
            view.sourcePath.setText(getDirectoryPathFromUri(sourceUri!!))
        }
        else if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            destinationUri = data?.data
            //view.path.setText(destinationUri?.path)

//            val path = ContentUriUtils.getFilePath(activity, destinationUri!!)
//            view.path.setText(path)
            view.path.setText(getDirectoryPathFromUri(destinationUri!!))
        }

//        if(resultCode == Activity.RESULT_OK){
//            sourceUri = data?.data
//            val path = getDirectoryPathFromUri(sourceUri)
//            when(requestCode){
//                0 -> view.sourcePath.setText(getDirectoryPathFromUri(sourceUri!!))
//                1 -> view.path.setText(getDirectoryPathFromUri(sourceUri!!))
//            }
//        }
    }

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

    private fun isFromExternalStorage(uri: Uri): Boolean {
        return uri.toString().contains("primary")
    }

    private fun onPositiveButtonClick(v: View){
        if (sourceUri == null || destinationUri == null || sourceUri.toString() == destinationUri.toString()) return
        activity.mediaMoveService(getDirectoryPathFromUri(sourceUri!!), getDirectoryPathFromUri(destinationUri!!))
        onComplete()
        dialog.cancel()

    }
}
