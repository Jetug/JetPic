package com.simplemobiletools.gallery.pro.ui.dialogs.raname

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.rename.RenameDialogBase
import com.simplemobiletools.gallery.pro.data.extensions.context.saveDirChanges
import com.simplemobiletools.gallery.pro.data.models.DirectoryGroup

class RenameGroupDialog(activity: BaseSimpleActivity, private val dirGroup: DirectoryGroup, val callback: (newPath: String) -> Unit): RenameDialogBase(activity) {
    init {
        view.apply {
            //rename_item_extension_hint.beGone()
            //rename_item_name.setText(dirGroup.name)

            hideHint()
            nameValue = dirGroup.name
        }
    }

    override fun onPositiveClick() {
        val newName = nameValue
        val groups = dirGroup.innerDirs

        groups.forEach {
            it.groupName = newName
        }

        activity.saveDirChanges(groups)
        callback(newName)
        builder.dismiss()
    }
}
