package com.simplemobiletools.gallery.pro.ui.dialogs.raname

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.rename.RenameDialogBase
import com.simplemobiletools.gallery.pro.data.extensions.context.renameGroupAsync
import com.simplemobiletools.gallery.pro.data.models.DirectoryGroup

class RenameGroupDialog(activity: BaseSimpleActivity, private val dirGroup: DirectoryGroup, val callback: (newPath: String) -> Unit): RenameDialogBase(activity) {
    init {
        view.apply {
            hideHint()
            nameValue = dirGroup.name
        }
    }

    override fun onPositiveClick() {
        val newName = nameValue
        activity.renameGroupAsync(dirGroup, newName)
        callback(newName)
        builder.dismiss()
    }
}
