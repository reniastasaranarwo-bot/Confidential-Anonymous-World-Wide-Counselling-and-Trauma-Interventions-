package org.cawcs.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.cawcs.app.data.Repository
import org.cawcs.app.payments.DemoPaymentGateway
import org.cawcs.app.security.AnonymousIdentity
import org.cawcs.app.ui.CawcsApp

class MainActivity : FragmentActivity() {

    private val unlocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Block screenshots, screen recording and the recent-apps preview.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()

        val identity = AnonymousIdentity(applicationContext)
        val repo = Repository()
        val payments = DemoPaymentGateway()

        if (!identity.exists || !identity.appLockEnabled || !canAuthenticate()) unlocked.value = true

        setContent {
            CawcsApp(
                identity = identity,
                repo = repo,
                payments = payments,
                unlocked = unlocked.value,
                onRequestUnlock = ::promptUnlock,
                onQuickExit = { finishAndRemoveTask() },
            )
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock whenever the app leaves the screen.
        if (canAuthenticate()) unlocked.value = false
    }

    private fun canAuthenticate() =
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

    private fun promptUnlock() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked.value = true
                }
            })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock")
                .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build()
        )
    }
}
