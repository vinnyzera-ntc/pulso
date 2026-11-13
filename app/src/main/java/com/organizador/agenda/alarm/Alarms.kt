package com.organizador.agenda.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Build

/** Volt do tema escuro (#2F95F5) para notificações, que ficam fora do tema Compose. */
const val NOTIFICATION_ACCENT_VOLT = 0xFF2F95F5.toInt()

/**
 * Alarme que acorda o aparelho em [atMillis]: exato quando o Android permite (USE_EXACT_ALARM;
 * no 12, SCHEDULE_EXACT_ALARM pode ter sido revogada), senão o mais perto possível.
 */
fun AlarmManager.setWakeUpAt(atMillis: Long, intent: PendingIntent) {
    val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms()
    try {
        if (exact) setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
        else setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
    } catch (_: SecurityException) {
        // Permissão revogada entre a checagem e o pedido.
        setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
    }
}
