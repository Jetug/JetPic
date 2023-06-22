  package com.simplemobiletools.gallery.pro.ui.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.launchIO
import com.simplemobiletools.gallery.pro.data.extensions.launchMain
import com.simplemobiletools.gallery.pro.data.helpers.JET
import kotlinx.android.synthetic.main.dialog_date_editing.view.*
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import org.joda.time.Period
import java.io.File
import kotlin.system.measureTimeMillis

class TaskDialog(val activity: BaseSimpleActivity, val onComplete: (Map<String, Long>) -> Unit = { _ ->}) {

    private val view = activity.layoutInflater.inflate(R.layout.dialog_date_editing, null)

    init {
        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, ::onPositiveButtonClick)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.edit_date) {
                    onCreate()
                }
            }
    }

    private fun onCreate(){

    }

    private fun onPositiveButtonClick(dialog: DialogInterface, id: Int) = launchIO{

    }
}
