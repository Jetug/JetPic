package com.simplemobiletools.commons.dialogs.rename

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_rename_items.*
import kotlinx.android.synthetic.main.dialog_rename_items.view.*
import java.util.*

class RenameItemsDialog(activity: BaseSimpleActivity, val paths: ArrayList<String>, val callback: () -> Unit): RenameItemsDialogBase(activity) {
    var ignoreClicks = false

    override fun onPositiveClick() {
        if (ignoreClicks) {
            return
        }

        val valueToAdd = view.rename_items_value.text.toString()
        val append = view.rename_items_radio_group.checkedRadioButtonId == view.rename_items_radio_append.id

        if (valueToAdd.isEmpty()) {
            callback()
            builder.dismiss()
            return
        }

        if (!valueToAdd.isAValidFilename()) {
            activity.toast(R.string.invalid_name)
            return
        }

        val validPaths = paths.filter { activity.getDoesFilePathExist(it) }
        val sdFilePath = validPaths.firstOrNull { activity.isPathOnSD(it) } ?: validPaths.firstOrNull()
        if (sdFilePath == null) {
            activity.toast(R.string.unknown_error_occurred)
            builder.dismiss()
            return
        }

        activity.handleSAFDialog(sdFilePath) {
            if (!it) {
                return@handleSAFDialog
            }

            ignoreClicks = true
            var pathsCnt = validPaths.size
            for (path in validPaths) {
                val fullName = path.getFilenameFromPath()
                var dotAt = fullName.lastIndexOf(".")
                if (dotAt == -1) {
                    dotAt = fullName.length
                }

                val name = fullName.substring(0, dotAt)
                val extension = if (fullName.contains(".")) ".${fullName.getFilenameExtension()}" else ""

                val newName = if (append) {
                    "$name$valueToAdd$extension"
                } else {
                    "$valueToAdd$fullName"
                }

                val newPath = "${path.getParentPath()}/$newName"

                if (activity.getDoesFilePathExist(newPath)) {
                    continue
                }

                activity.renameFile(path, newPath, true) { success, useAndroid30Way ->
                    if (success) {
                        pathsCnt--
                        if (pathsCnt == 0) {
                            callback()
                            builder.dismiss()
                        }
                    } else {
                        ignoreClicks = false
                        activity.toast(R.string.unknown_error_occurred)
                        builder.dismiss()
                    }
                }
                builder.dismiss()
            }
        }
    }
}
