package com.organizador.agenda.focus

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Serviço em primeiro plano do modo foco: mantém o processo vivo e a notificação com a contagem
 * enquanto houver uma sessão ativa, mesmo com a tela apagada. Não guarda estado: só reflete o
 * [FocusController] e repassa as ações dos botões da notificação.
 */
@AndroidEntryPoint
class FocusService : LifecycleService() {
    @Inject lateinit var controller: FocusController
    @Inject lateinit var notifier: FocusNotifier

    override fun onCreate() {
        super.onCreate()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, FocusNotifier.ID_ONGOING, notifier.ongoing(controller.state.value), type)
        val manager = getSystemService(NotificationManager::class.java)
        lifecycleScope.launch {
            controller.state.collect { state ->
                if (!state.active) {
                    ServiceCompat.stopForeground(this@FocusService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    manager.notify(FocusNotifier.ID_ONGOING, notifier.ongoing(state))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_PAUSE -> controller.pause()
            ACTION_RESUME -> controller.resume()
            ACTION_STOP -> controller.stop()
        }
        // Se o sistema matar o processo, o estado (em memória) se perde: não adianta recriar o serviço.
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_PAUSE = "com.organizador.agenda.focus.PAUSE"
        const val ACTION_RESUME = "com.organizador.agenda.focus.RESUME"
        const val ACTION_STOP = "com.organizador.agenda.focus.STOP"

        fun start(context: Context) = ContextCompat.startForegroundService(context, Intent(context, FocusService::class.java))
    }
}

/** Alarme exato do fim da fase: acorda o aparelho e avisa o controlador. */
@AndroidEntryPoint
class FocusAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: FocusController

    override fun onReceive(context: Context, intent: Intent) {
        controller.onPhaseDue()
    }
}
