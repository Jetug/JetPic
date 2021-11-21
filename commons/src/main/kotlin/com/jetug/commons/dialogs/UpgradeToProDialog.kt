package com.jetug.commons.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.jetug.commons.R
import com.jetug.commons.extensions.launchUpgradeToProIntent
import com.jetug.commons.extensions.launchViewIntent
import com.jetug.commons.extensions.setupDialogStuff
import kotlinx.android.synthetic.main.dialog_upgrade_to_pro.view.*

class UpgradeToProDialog(val activity: Activity) {

    init {
        val view = activity.layoutInflater.inflate(R.layout.dialog_upgrade_to_pro, null).apply {
            upgrade_to_pro.text = activity.getString(R.string.upgrade_to_pro_long)
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.upgrade) { dialog, which -> upgradeApp() }
            .setNeutralButton(R.string.more_info, null)     // do not dismiss the dialog on pressing More Info
            .setNegativeButton(R.string.cancel, null)
            .create().apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false)
                getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                    moreInfo()
                }
            }
    }

    private fun upgradeApp() {
        activity.launchUpgradeToProIntent()
    }

    private fun moreInfo() {
        activity.launchViewIntent("https://medium.com/@tibbi/some-simple-mobile-tools-apps-are-becoming-paid-d053268f0fb2")
    }
}
