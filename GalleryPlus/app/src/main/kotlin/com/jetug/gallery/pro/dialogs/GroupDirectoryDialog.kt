package com.jetug.gallery.pro.dialogs

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.jetug.commons.R
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_group_dirs.view.*

class GroupDirectoryDialog(val activity: BaseSimpleActivity, val callback: (name: String) -> Unit = {}) {

    private val view = activity.layoutInflater.inflate(com.jetug.gallery.pro.R.layout.dialog_group_dirs, null)

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
