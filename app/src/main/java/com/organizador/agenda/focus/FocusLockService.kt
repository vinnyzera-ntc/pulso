package com.organizador.agenda.focus

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Serviço de acessibilidade com um único uso: o cadeado do descanso de tela do foco bloqueia o
 * celular (GLOBAL_ACTION_LOCK_SCREEN), como o botão lateral. Não lê a tela nem recebe eventos
 * (ver res/xml/focus_lock_accessibility.xml). Diferente do administrador do dispositivo, o
 * bloqueio assim não exige a senha: digital e rosto continuam desbloqueando.
 */
class FocusLockService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        _running.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    private fun disconnect() {
        if (instance?.get() === this) instance = null
        _running.value = false
    }

    companion object {
        private var instance: WeakReference<FocusLockService>? = null
        private val _running = MutableStateFlow(false)

        /** true enquanto o sistema mantém o serviço ligado (ativado em Acessibilidade). */
        val running: StateFlow<Boolean> = _running

        /** GLOBAL_ACTION_LOCK_SCREEN existe a partir do Android 9. */
        val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /** Atraso até reacender a tela: o bloqueio precisa ter apagado a tela antes. */
        private const val WAKE_DELAY_MS = 700L
        private const val WAKE_HOLD_MS = 3_000L

        /**
         * Bloqueia o celular e, logo depois, reacende a tela por alguns segundos: a tela do foco
         * (com showWhenLocked) aparece por cima da tela de bloqueio. false = serviço desligado.
         */
        fun lock(context: Context): Boolean {
            if (!supported) return false
            val service = instance?.get() ?: return false
            if (!service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)) return false
            val app = context.applicationContext
            Handler(Looper.getMainLooper()).postDelayed({ wakeScreen(app) }, WAKE_DELAY_MS)
            return true
        }

        /** Wake lock curto que acende a tela; ela segue a regra normal de apagar depois. */
        @Suppress("DEPRECATION") // SCREEN_BRIGHT_WAKE_LOCK: a única forma de acender a tela por um wake lock.
        private fun wakeScreen(context: Context) {
            val power = context.getSystemService(PowerManager::class.java) ?: return
            power.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "Pulso:focusLock",
            ).acquire(WAKE_HOLD_MS)
        }

        /** Se o usuário ativou o serviço em Acessibilidade (mesmo antes de o sistema ligá-lo). */
        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false
            val me = ComponentName(context, FocusLockService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
        }

        fun settingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
