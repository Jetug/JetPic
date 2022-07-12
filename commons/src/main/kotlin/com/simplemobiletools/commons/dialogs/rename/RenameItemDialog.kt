package com.simplemobiletools.commons.dialogs.rename

import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import kotlinx.android.synthetic.main.dialog_rename_item.view.*
import java.util.*

class RenameItemDialog(activity: BaseSimpleActivity, val path: String, val callback: (newPath: String) -> Unit): RenameDialogBase(activity) {

    var ignoreClicks = false
    val fullName = path.getFilenameFromPath()
    val dotAt = fullName.lastIndexOf(".")
    var name = fullName

    init {
        view.apply {
            if (dotAt > 0 && !activity.getIsPathDirectory(path)) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                rename_item_extension.setText(extension)
            } else {
                rename_item_extension_hint.beGone()
            }

            rename_item_name.setText(name)
        }
    }

    override fun onPositiveClick() {
        if (ignoreClicks) {
            return
        }

        var newName = view.rename_item_name.value
        val newExtension = view.rename_item_extension.value

        if (newName.isEmpty()) {
            activity.toast(R.string.empty_name)
            return
        }

        if (!newName.isAValidFilename()) {
            activity.toast(R.string.invalid_name)
            return
        }

        val updatedPaths = ArrayList<String>()
        updatedPaths.add(path)
        if (!newExtension.isEmpty()) {
            newName += ".$newExtension"
        }

        if (!activity.getDoesFilePathExist(path)) {
            activity.toast(String.format(activity.getString(R.string.source_file_doesnt_exist), path))
            return
        }

        val newPath = "${path.getParentPath()}/$newName"

        if (path == newPath) {
            activity.toast(R.string.name_taken)
            return
        }

        if (!path.equals(newPath, ignoreCase = true) && activity.getDoesFilePathExist(newPath)) {
            activity.toast(R.string.name_taken)
            return
        }

        updatedPaths.add(newPath)
        ignoreClicks = true
        activity.renameFile(path, newPath, false) { success, useAndroid30Way ->
            ignoreClicks = false
            if (success) {
                callback(newPath)
                builder.dismiss()
            } else {
                activity.toast(R.string.unknown_error_occurred)
            }
        }
    }
}
