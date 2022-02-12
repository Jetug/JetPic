package com.jetug.gallery.pro.dialogs

import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.jetug.commons.activities.BaseSimpleActivity
import com.jetug.commons.extensions.setupDialogStuff
import com.jetug.commons.extensions.showKeyboard
import com.jetug.commons.extensions.value
import com.jetug.gallery.pro.R
import kotlinx.android.synthetic.main.dialog_custom_aspect_ratio.view.*

class CustomAspectRatioDialog(val activity: BaseSimpleActivity, val defaultCustomAspectRatio: Pair<Float, Float>?, val callback: (aspectRatio: Pair<Float, Float>) -> Unit) {
    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_custom_aspect_ratio, null).apply {
            aspect_ratio_width.setText(defaultCustomAspectRatio?.first?.toInt()?.toString() ?: "")
            aspect_ratio_height.setText(defaultCustomAspectRatio?.second?.toInt()?.toString() ?: "")
        }

        AlertDialog.Builder(activity)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
                    activity.setupDialogStuff(view, this) {
                        showKeyboard(view.aspect_ratio_width)
                        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                            val width = getViewValue(view.aspect_ratio_width)
                            val height = getViewValue(view.aspect_ratio_height)
                            callback(Pair(width, height))
                            dismiss()
                        }
                    }
                }
    }

    private fun getViewValue(view: EditText): Float {
        val textValue = view.value
        return if (textValue.isEmpty()) 0f else textValue.toFloat()
    }
}
