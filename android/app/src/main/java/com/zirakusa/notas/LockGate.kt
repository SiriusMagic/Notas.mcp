package com.zirakusa.notas

import android.app.KeyguardManager
import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** «Bloqueado» opens only after the phone's own lock (fingerprint, face, PIN, pattern or password), like Luma's vault. */
object LockGate {
    fun ask(activity: FragmentActivity, onOpen: () -> Unit) {
        if (activity.getSystemService(KeyguardManager::class.java)?.isDeviceSecure != true) {
            Toast.makeText(activity, R.string.no_screen_lock, Toast.LENGTH_LONG).show()
            return
        }
        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOpen()
        })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(activity.getString(R.string.locked))
            .setSubtitle(activity.getString(R.string.unlock_subtitle)).setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL).build())
    }
}
