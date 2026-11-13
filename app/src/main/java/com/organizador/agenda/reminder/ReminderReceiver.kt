package com.organizador.agenda.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.domain.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/** Alarme do próximo lembrete e botões da notificação ("Concluir", "Adiar"). Não exportado. */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: TaskReminderScheduler
    @Inject lateinit var notifier: ReminderNotifier
    @Inject lateinit var tasks: TaskRepository
    @Inject lateinit var clock: Clock
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val pending = goAsync()
        scope.launch {
            try {
                withTimeoutOrNull(TIMEOUT_MS) {
                    when (intent.action) {
                        ACTION_DONE -> {
                            notifier.cancel(taskId)
                            tasks.setCompleted(taskId, true)
                        }
                        ACTION_SNOOZE -> {
                            notifier.cancel(taskId)
                            val minutes = intent.getIntExtra(EXTRA_MINUTES, ReminderNotifier.SNOOZE_SHORT).toLong()
                            val until = LocalDateTime.now(clock).withSecond(0).withNano(0).plusMinutes(minutes)
                            tasks.snoozeReminder(intent.getLongExtra(EXTRA_REMINDER_ID, -1), until)
                        }
                    }
                    scheduler.refresh()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.organizador.agenda.reminder.FIRE"
        const val ACTION_DONE = "com.organizador.agenda.reminder.DONE"
        const val ACTION_SNOOZE = "com.organizador.agenda.reminder.SNOOZE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_MINUTES = "minutes"

        /** goAsync dá ~10 s; a conferência leva milissegundos. */
        private const val TIMEOUT_MS = 8_000L
    }
}

/**
 * Religar o aparelho, atualizar o app ou mudar a hora/o fuso apaga ou desloca o alarme:
 * confere tudo de novo (o que se perdeu com o aparelho desligado toca agora).
 */
@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: TaskReminderScheduler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED) return
        val pending = goAsync()
        scope.launch {
            try {
                withTimeoutOrNull(8_000L) { scheduler.refresh() }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
