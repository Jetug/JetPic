package com.jetug.commons.dialogs

import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import com.jetug.commons.R
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.extensions.applyColorFilter
import com.jetug.commons.extensions.baseConfig
import com.jetug.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_call_confirmation.view.*

class CallConfirmationDialog(val activity: BaseSimpleActivity, val callee: String, private val callback: () -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_call_confirmation, null)

    init {
        view.call_confirm_phone.applyColorFilter(activity.baseConfig.textColor)
        AlertDialog.Builder(activity)
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                val title = String.format(activity.getString(R.string.call_person), callee)
                activity.setupDialogStuff(view, this, titleText = title) {
                    view.call_confirm_phone.apply {
                        startAnimation(AnimationUtils.loadAnimation(activity, R.anim.pulsing_animation))
                        setOnClickListener {
                            callback.invoke()
                            dismiss()
                        }
                    }
                }
            }
    }
}
