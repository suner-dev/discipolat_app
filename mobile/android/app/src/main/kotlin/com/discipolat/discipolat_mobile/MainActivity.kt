package com.discipolat.discipolat_mobile

import android.annotation.TargetApi
import android.os.Build
import android.view.WindowInsetsController
import android.view.WindowManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * Handler Flutter Platform Channel pour la [protection d'écran].
 *
 * ACTIVE VRAIMENT [FLAG_SECURE] sur Android (ce que faisait défaut le
 * `SystemChrome.setEnabledSystemUIMode` utilisé précédemment du côté Flutter :
 * il n’empêche que l’affichage des barres système, pas les captures d’écran).
 *
 * Canal : `discipolat/secure_screen`
 * Méthode : `setSecureFlag` avec argument booléen `enabled`.
 *
 * Toutes les options d’authentification (2FA TOTP, biométrie) délèguent à
 * Flutter via le channel `discipolat/biometric` existant.
 */
class MainActivity : FlutterActivity() {

    private val CHANNEL = "discipolat/secure_screen"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "setSecureFlag" -> {
                        val enabled = call.argument<Boolean>("enabled") ?: true
                        setSecureFlag(enabled)
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    /**
     * Active ou désactive [FLAG_SECURE] : empêche les captures d’écran et
     * l’enregistrement d’écran tant que l’activité est au premier plan.
     */
    @TargetApi(Build.VERSION_CODES.R)
    private fun setSecureFlag(enabled: Boolean) {
        val window = window
        val insetsController = window.insetsController
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            insetsController?.hide(
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_FROM_TOUCH,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

