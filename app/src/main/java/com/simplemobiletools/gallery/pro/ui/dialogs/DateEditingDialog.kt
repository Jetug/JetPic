package com.example.unipicdev.views.dialogs

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.view.View
import android.widget.*
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.data.jetug.setLastModified
import kotlinx.android.synthetic.main.dialog_date_editing.view.*
import org.joda.time.*
import java.io.File

class DateEditingDialog(val activity: BaseSimpleActivity, val paths: ArrayList<String>,
                        val onComplete: (DateTime, Period) -> Unit = { _, _ ->}) {
    private val defaultStep = Period.years(0).withMonths(0).withDays(0).withHours(0).withMinutes(1).withSeconds(0)
    private var isAddition = false

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
        initViews()
        setDateOfFile()
        setStep(defaultStep)
    }

    private fun initViews(){
        view.apply {
            if(paths.size == 1){
                rg_operations.visibility = View.GONE
                stepDateTimeHolder.visibility = View.GONE
            }

            initialTimePicker.setIs24HourView(true)
            stepTimePicker.setIs24HourView(true)
            initSeconds.minValue = 0
            initSeconds.maxValue = 59
            stepSeconds.minValue = 0
            stepSeconds.maxValue = 59

            rb_current.setOnClickListener{ setDateCurrent()}
            rb_of_file.setOnClickListener{ setDateOfFile() }
            plus.setOnClickListener{ isAddition = true }
            minus.setOnClickListener{ isAddition = false }
        }
    }

    private fun onPositiveButtonClick(dialog: DialogInterface, id: Int) {
        val initDate = getInitialDate()
        val step = getStep()

        var buffDate = initDate
        for (path in paths) {
            File(path).setLastModified(initDate)
            buffDate = if (isAddition) buffDate.plus(step) else buffDate.minus(step)
        }

        onComplete(initDate, step)
    }

    private fun setDateOfFile(){
        if(paths.size > 0){
            val dateTime = DateTime(File(paths[0]).lastModified())
            setDate(dateTime.toLocalDate())
            setTime(dateTime.toLocalTime())
        }
    }

    private fun setDateCurrent(){
        setDate(LocalDate.now())
        setTime(LocalTime.now())
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

    private fun setDate(date: LocalDate){
        val year  = date.year().get()
        val month = date.monthOfYear().get() - 1
        val day   = date.dayOfMonth().get()

        view.apply {
            initialDatePicker.updateDate(year,month,day)
        }
    }

    private fun setTime(time: LocalTime){
        val hour   = time.hourOfDay
        val minute = time.minuteOfHour
        val second = time.secondOfMinute

        view.apply {
            initialTimePicker.hour = hour
            initialTimePicker.minute = minute
            initSeconds.value = second
        }
    }


}
