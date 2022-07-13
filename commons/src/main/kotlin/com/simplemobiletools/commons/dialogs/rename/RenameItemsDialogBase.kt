package com.simplemobiletools.commons.dialogs.rename

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_rename_items.*
import kotlinx.android.synthetic.main.dialog_rename_items.view.*
import java.util.ArrayList

abstract class RenameItemsDialogBase(val activity: BaseSimpleActivity) {
    @SuppressLint("InflateParams")
    val view = activity.layoutInflater.inflate(R.layout.dialog_rename_items, null)
    val builder: AlertDialog

    val valueToAdd get() = view.rename_items_value.text.toString()
    val append get() = view.rename_items_radio_group.checkedRadioButtonId == view.rename_items_radio_append.id

    init {
        builder = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.rename) {
                    showKeyboard(view.rename_items_value)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        onPositiveClick()
                    }
                }
            }
    }

    abstract fun onPositiveClick()
}
