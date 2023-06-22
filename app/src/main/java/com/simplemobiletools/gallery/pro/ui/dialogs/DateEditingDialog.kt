package com.simplemobiletools.gallery.pro.ui.dialogs

import android.content.DialogInterface
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.view.View
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.extensions.launchIO
import com.simplemobiletools.gallery.pro.data.extensions.launchMain
import com.simplemobiletools.gallery.pro.data.helpers.JET
import kotlinx.android.synthetic.main.dialog_date_editing.view.*
import org.joda.time.*
import java.io.File
import kotlin.system.measureTimeMillis

class DateEditingDialog(val activity: BaseSimpleActivity, val paths: ArrayList<String>,
                        val onComplete: (Map<String, Long>) -> Unit = { _ ->}) {
    private val defaultStep = Period.years(0).withMonths(0).withDays(0).withHours(0).withMinutes(1).withSeconds(0)
    private var isAddition = false

    private val view = activity.layoutInflater.inflate(R.layout.dialog_date_editing, null)

    init { launchMain {
        val time = measureTimeMillis {
            AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, ::onPositiveButtonClick)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this, R.string.edit_date) {
                        onCreate()
                    }
                }
        }
        Log.e(JET,"DateEditingDialog.init() $time ms")
    }}

    private fun onCreate(){
        val createTime = measureTimeMillis {
            initViews()
            setDateOfFile()
            setStep(defaultStep)
        }
        Log.e(JET,"DateEditingDialog.onCreate() $createTime ms")
    }

    private fun initViews() = launchMain {
        val time = measureTimeMillis {
            view.apply {
                if (paths.size == 1) {
                    rg_operations.visibility = View.GONE
                    stepDateTimeHolder.visibility = View.GONE
                }

                initialTimePicker.setIs24HourView(true)
                stepTimePicker.setIs24HourView(true)

                initSeconds.maxValue = 59
                stepSeconds.maxValue = 59
                stepDay.maxValue = 59
                stepMonth.maxValue = 59
                stepYear.maxValue = 59

                rb_current.setOnClickListener { setDateCurrent() }
                rb_of_file.setOnClickListener { setDateOfFile() }
                plus.setOnClickListener { isAddition = true }
                minus.setOnClickListener { isAddition = false }
            }
        }
        Log.e(JET,"DateEditingDialog.initViews() $time ms")
    }

    private fun onPositiveButtonClick(dialog: DialogInterface, id: Int) = launchIO{
        activity.toast(R.string.date_editing)
        val dateMap = mutableMapOf<String, Long>()
        val initDate = getInitialDate()
        val step = getStep()
        var date = initDate

        for (path in paths) {
            val file = File(path)
            if(file.exists()) {
                val dateLong = date.toDate().time
                file.setLastModified(dateLong)
                dateMap.put(path, dateLong)
                date = if (isAddition) date.plus(step) else date.minus(step)
            }
        }

        activity.toast(R.string.date_editing_success)
        onComplete(dateMap)
    }

    private fun setDateOfFile(){
        if(paths.size > 0){
            val dateTime = DateTime(File(paths[0]).lastModified())
            setDateTime(dateTime.toLocalDateTime())
        }
    }

    private fun setDateCurrent(){
        setDateTime(LocalDateTime.now())
    }

    private fun getInitialDate(): DateTime{
        val dateTime : DateTime
        view.apply {
            val year = initialDatePicker.year
            val month = initialDatePicker.month + 1
            val day = initialDatePicker.dayOfMonth
            val hour = initialTimePicker.hour
            val minute = initialTimePicker.minute
            val second = initSeconds.value
            dateTime = DateTime(year, month, day, hour, minute, second)
        }
        return dateTime
    }

    private fun setDateTime(date: LocalDateTime){
        val year   = date.year()        .get()
        val month  = date.monthOfYear() .get() - 1
        val day    = date.dayOfMonth()  .get()
        val hour   = date.hourOfDay
        val minute = date.minuteOfHour
        val second = date.secondOfMinute

        view.apply {
            initialDatePicker.updateDate(year,month,day)
            initialTimePicker.hour = hour
            initialTimePicker.minute = minute
            initSeconds.value = second
        }
    }

    private fun getStep(): Period{
        val step : Period
        view.apply {
            val years = stepDay.value
            val months = stepMonth.value
            val days = stepYear.value
            val hours = stepTimePicker.hour
            val minutes = stepTimePicker.minute
            val seconds = stepSeconds.value
            step = Period.years(years).withMonths(months).withDays(days).withHours(hours).withMinutes(minutes).withSeconds(seconds)
        }
        return step
    }

    private fun setStep(date: Period){
        view.apply {
            stepDay  .value       = date.days
            stepMonth.value       = date.months
            stepYear .value       = date.years
            stepTimePicker.hour   = date.hours
            stepTimePicker.minute = date.minutes
            stepSeconds.value     = date.seconds
        }
    }
}
