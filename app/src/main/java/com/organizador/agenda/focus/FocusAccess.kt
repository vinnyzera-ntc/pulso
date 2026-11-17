package com.organizador.agenda.focus

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** Acessos que o usuário dá nas configurações do sistema para as opções do modo foco. */
data class FocusAccess(
    /** Serviço de acessibilidade do cadeado ativado. */
    val lockService: Boolean,
    /** Acesso ao Não perturbe (ACCESS_NOTIFICATION_POLICY). */
    val doNotDisturb: Boolean,
    /** Acesso às notificações (para repassar as dos apps liberados). */
    val notificationListener: Boolean,
) {
    companion object {
        fun read(context: Context): FocusAccess = FocusAccess(
            lockService = FocusLockService.isEnabled(context),
            doNotDisturb = context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true,
            notificationListener = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context),
        )
    }
}
