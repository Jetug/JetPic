package com.simplemobiletools.commons.dialogs.rename

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_rename_item.view.*
import java.util.ArrayList

abstract class RenameDialogBase (val activity: BaseSimpleActivity)  {
    val view: View
    val builder: AlertDialog
    var nameValue: String
        set(value) {
            view.rename_item_name.setText(value)
        }
        get() = view.rename_item_name.value

    init {
        view = activity.layoutInflater.inflate(R.layout.dialog_rename_item, null)
        builder = AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.rename) {
                    showKeyboard(view.rename_item_name)
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        onPositiveClick()
                    }
                }
            }
    }

    abstract fun onPositiveClick()

    protected fun hideHint(){
        view.rename_item_extension_hint.beGone()
    }


}
