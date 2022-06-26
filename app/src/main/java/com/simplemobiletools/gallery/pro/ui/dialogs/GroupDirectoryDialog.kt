package com.simplemobiletools.gallery.pro.ui.dialogs

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_group_dirs.view.*

class GroupDirectoryDialog(val activity: BaseSimpleActivity, val callback: (name: String) -> Unit = {}) {

    private val view = activity.layoutInflater.inflate(com.simplemobiletools.gallery.pro.R.layout.dialog_group_dirs, null)

    init {
        var ignoreClicks = false

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, ::onPositiveButton)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, R.string.rename) {
                    showKeyboard(view.group_name_value)
                }
            }
    }

    private fun onPositiveButton(dialog: DialogInterface, id: Int){
        val name = view.group_name_value.value
        callback(name)
    }
}
