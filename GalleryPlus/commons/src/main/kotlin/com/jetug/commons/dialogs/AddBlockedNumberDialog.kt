package com.jetug.commons.dialogs

import androidx.appcompat.app.AlertDialog
import com.jetug.commons.R
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.extensions.*
import com.jetug.commons.models.BlockedNumber
import kotlinx.android.synthetic.main.dialog_add_blocked_number.view.*

class AddBlockedNumberDialog(val activity: BaseSimpleActivity, val originalNumber: BlockedNumber? = null, val callback: () -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_add_blocked_number, null).apply {
            if (originalNumber != null) {
                add_blocked_number_edittext.setText(originalNumber.number)
            }
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        showKeyboard(view.add_blocked_number_edittext)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val newBlockedNumber = view.add_blocked_number_edittext.value
                            if (originalNumber != null && newBlockedNumber != originalNumber.number) {
                                activity.deleteBlockedNumber(originalNumber.number)
                            }

                            if (newBlockedNumber.isNotEmpty()) {
                                activity.addBlockedNumber(newBlockedNumber)
                            }

                            callback()
                            dismiss()
                        }
                    }
                }
    }
}
