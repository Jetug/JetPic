  package com.simplemobiletools.gallery.pro.ui.dialogs

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.flexbox.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.views.MyTextView
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.launchIO
import com.simplemobiletools.gallery.pro.data.extensions.launchMain
import com.simplemobiletools.gallery.pro.data.helpers.JET
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
        view.apply {
            for (i in 1..10) {
                val textView = TextView(activity)
                textView.text = "TextView $i"
                textView.setBackgroundColor(Color.GRAY)
                textView.setTextColor(Color.WHITE)

                val layoutParams = FlowLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // Установите нужные вам значения межстрочного и межсекционного расстояния
                layoutParams.setMargins(10, 10, 10, 10)

                // Добавьте TextView на экран
                flowLayout.addView(textView, layoutParams)
            }

            addBtn.setOnClickListener {
//                val textView = createTextView("")
//                flowLayout.addView(textView)

                val textView = MyTextView(activity)
                textView.text = "TextView dyb"
//                textView.setBackgroundColor(Color.GRAY)
//                textView.setTextColor(Color.WHITE)

                val layoutParams = FlowLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )

                // Установите нужные вам значения межстрочного и межсекционного расстояния
                layoutParams.setMargins(10, 10, 10, 10)

                // Добавьте TextView на экран
                flowLayout.addView(textView, layoutParams)
            }
        }
    }

    private fun createTextView(text: String): MyTextView {
        val textView = MyTextView(activity)
        val layoutParams = FlexboxLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        //layoutParams.setMargins(8, 8, 8, 8)
        textView.layoutParams = layoutParams
        textView.text = text
        return textView
    }

    private fun onPositiveButtonClick(dialog: DialogInterface, id: Int) = launchIO{

    }
}
