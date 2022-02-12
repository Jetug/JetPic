package com.jetug.commons.views

import android.content.Context
import android.util.AttributeSet
import androidx.biometric.auth.AuthPromptHost
import androidx.constraintlayout.widget.ConstraintLayout
import com.jetug.commons.extensions.showBiometricPrompt
import com.jetug.commons.extensions.updateTextColors
import com.jetug.commons.interfaces.HashListener
import com.jetug.commons.interfaces.SecurityTab
import kotlinx.android.synthetic.main.tab_biometric_id.view.*

class BiometricIdTab(context: Context, attrs: AttributeSet) : ConstraintLayout(context, attrs), SecurityTab {
    private lateinit var hashListener: HashListener
    private lateinit var biometricPromptHost: AuthPromptHost

    override fun onFinishInflate() {
        super.onFinishInflate()
        context.updateTextColors(biometric_lock_holder)

        open_biometric_dialog.setOnClickListener {
            biometricPromptHost.activity?.showBiometricPrompt(successCallback = hashListener::receivedHash)
        }
    }

    override fun initTab(requiredHash: String, listener: HashListener, scrollView: MyScrollView, biometricPromptHost: AuthPromptHost) {
        this.biometricPromptHost = biometricPromptHost
        hashListener = listener
    }

    override fun visibilityChanged(isVisible: Boolean) {}
}
