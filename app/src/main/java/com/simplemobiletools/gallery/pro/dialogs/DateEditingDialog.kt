package com.example.unipicdev.views.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.VIEW_TYPE_GRID
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.extensions.*
import com.simplemobiletools.gallery.pro.jetug.setLastModified
import com.simplemobiletools.gallery.pro.models.DirectoryGroup
import com.simplemobiletools.gallery.pro.models.FolderItem
import kotlinx.android.synthetic.main.dialog_date_editing.view.*
import org.joda.time.*
import java.io.File
import java.util.ArrayList

class DateEditingDialog(val activity: BaseSimpleActivity, val paths: ArrayList<String>,
                        val onComplete: (DateTime, Period) -> Unit = { _, _ ->}): AdapterView.OnItemSelectedListener  {
    private var dialog: androidx.appcompat.app.AlertDialog
    private var view = activity.layoutInflater.inflate(R.layout.dialog_date_editing, null)

    private lateinit var datePicker: DatePicker
    private lateinit var timePicker: TimePicker
    private lateinit var secondsNP: NumberPicker
    private var isAddition = false

    init {
        val builder = androidx.appcompat.app.AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, ::onPositiveButtonClick)
            .setNegativeButton(R.string.cancel, null)

        dialog = builder.create().apply {
            activity.setupDialogStuff(view, this, R.string.edit_date) {
                onCreate(context)
            }
        }
    }

    fun onCreate(context: Context){
        datePicker = view.initialDatePicker
        timePicker = view.initialTimePicker
        secondsNP = view.initSeconds

        val currentDate = LocalDate.now()
        val year = currentDate.year().get()
        val month = currentDate.monthOfYear().get() - 1
        val day = currentDate.dayOfMonth().get()

        val currentTime = LocalTime.now()
        val hour = currentTime.hourOfDay
        val minute = currentTime.minuteOfHour
        val second = currentTime.secondOfMinute

        view.apply {
            if(paths.size == 1){
                spinner.visibility = View.GONE
                stepDateTimeHolder.visibility = View.GONE
            }

            initialDatePicker.updateDate(year,month,day)
            initialTimePicker.setIs24HourView(true)
            stepTimePicker.setIs24HourView(true)
            initSeconds.minValue = 0
            initSeconds.maxValue = 59
            stepSeconds.minValue = 0
            stepSeconds.maxValue = 59

            initialTimePicker.hour = hour
            initialTimePicker.minute = minute
            initSeconds.value = second
        }




        val spinner = view.spinner
        val adapter = ArrayAdapter.createFromResource(
            context,
            R.array.signs,
            R.layout.item_spinner
        )
        spinner.adapter = adapter
        spinner.onItemSelectedListener = this
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun onPositiveButtonClick(dialog: DialogInterface, id: Int) {
        val year = datePicker.year
        val month = datePicker.month + 1
        val day = datePicker.dayOfMonth
        val hour = timePicker.hour
        val minute = timePicker.minute
        val second = secondsNP.value

        val initDate = DateTime(year, month, day, hour, minute, second)
        val step = Period.years(0).withMonths(0).withDays(0).withHours(0).withMinutes(1).withSeconds(0)

        var buffDate = initDate
        for (path in paths) {
            File(path).setLastModified(buffDate)
            buffDate = if (isAddition) buffDate.plus(step) else buffDate.minus(step)
        }

        onComplete(initDate, step)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }
}

//class DateEditingDialog2(val paths: ArrayList<String>, val onComplete: (DateTime, Period) -> Unit = { _, _ ->}): DialogFragment(){
//
//    private lateinit var datePicker: DatePicker
//    private lateinit var timePicker: TimePicker
//    private lateinit var secondsNP: NumberPicker
//    private var isAddition = false
//
//    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
//        val inflater = requireActivity().layoutInflater
//        val view: View = inflater.inflate(R.layout.dialog_date_editing, null)
//        val alertDialogBuilder = AlertDialog.Builder(activity)
//
////        if(dialog != null) {
////            setDialogMatchParent(dialog!!)
////            Toast.makeText(context, "yes", Toast.LENGTH_SHORT).show()
////        }
//
////        setMaxDialogWidth()
//
////        val dl: Dialog? = dialog
////        val win = dl?.window
////        val lp = WindowManager.LayoutParams()
////
////        if(win != null && dl != null) {
////            lp.copyFrom(win.attributes)
////            lp.width = WindowManager.LayoutParams.MATCH_PARENT
////            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
////            lp.gravity = Gravity.CENTER
////
////            dialog!!.window!!.attributes = lp
////        }
//
//        datePicker = view.findViewById(R.id.initialDatePicker)
//        timePicker = view.findViewById(R.id.initialTimePicker)
//        secondsNP = view.findViewById(R.id.initSeconds)
//
//        val currentDate = LocalDate.now()
//        val y = currentDate.year().get()
//        val mm = currentDate.monthOfYear().get() - 1
//        val d = currentDate.dayOfMonth().get()
//
//        val currentTime = LocalTime.now()
//        val h = currentTime.hourOfDay
//        val m = currentTime.minuteOfHour
//        val s = currentTime.secondOfMinute
//
//        datePicker.updateDate(y,mm,d)
//        timePicker.setIs24HourView(true)
//        secondsNP.minValue = 0
//        secondsNP.maxValue = 59
//
//        timePicker.hour = h
//        timePicker.minute = m
//        secondsNP.value = s
//
//        return alertDialogBuilder
//            .setTitle("Изменить дату")
//            .setView(view)
//            .setPositiveButton("Ок", ::onPositiveButtonClick)
//            .setNegativeButton("Отмена") { dialog, id ->
//                dialog.cancel()
//            }
//            .create()
//    }
//
//
//    fun setDialogMatchParent(dialog: Dialog){
//
//        val window = dialog.getWindow()
//        val ttt = Context.WINDOW_SERVICE
//
//        if(window != null && context != null) {
//            val wm: WindowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
//            val dm: DisplayMetrics = DisplayMetrics();
//            wm.getDefaultDisplay().getMetrics(dm);
//            val width = dm.widthPixels;// screen width (pixels)
//            val height = dm.heightPixels; // screen height (pixels)
//            val density = dm.density;//screen density (0.75 / 1.0 / 1.5)
//            val densityDpi = dm.densityDpi;//screen density dpi (120 / 160 / 240)
//            val layoutParams: WindowManager.LayoutParams = window.getAttributes()
//            // This place can use the ViewGroup.LayoutParams.MATCH_PARENT property, you can try to see if there is any effect
//            layoutParams.width = width
//            layoutParams.height = height
//            window.setAttributes(layoutParams)
//        }
//    }
//
//
////    private fun setMaxDialogWidth(){
////        if(activity != null && requireActivity().window != null) {
////            val params: WindowManager.LayoutParams = activity?.window.attributes
////            params.width = WindowManager.LayoutParams.MATCH_PARENT
////            activity?.window.attributes = params
////        }
////    }
//
//    @RequiresApi(Build.VERSION_CODES.M)
//    private fun onPositiveButtonClick(dialog: DialogInterface, id: Int){
//        val f = dialog as Dialog
//        val year = datePicker.year
//        val month = datePicker.month + 1
//        val day = datePicker.dayOfMonth
//        val hour = timePicker.hour
//        val minute = timePicker.minute
//        val second = secondsNP.value
//
//        val initDate = DateTime(year, month, day, hour, minute, second)
//        val step = Period.years(0).withMonths(0).withDays(0).withHours(0).withMinutes(1).withSeconds(0)
//
//        var buffDate = initDate
//        for(path in paths){
//            File(path).setLastModified(buffDate)
//            buffDate = if(isAddition)  buffDate.plus(step) else buffDate.minus(step)
//        }
//
//        onComplete(initDate, step)
//    }
//}
