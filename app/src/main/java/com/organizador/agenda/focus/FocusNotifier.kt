package com.organizador.agenda.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.organizador.agenda.MainActivity
import com.organizador.agenda.R
import com.organizador.agenda.domain.focus.FocusMachine
import com.organizador.agenda.domain.focus.FocusState
import com.organizador.agenda.domain.model.FocusKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Notificações do modo foco: a contínua (do serviço) e o aviso de fim de fase. */
@Singleton
class FocusNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TIMER, context.getString(R.string.focus_channel_timer), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.focus_channel_timer_desc)
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, context.getString(R.string.focus_channel_done), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.focus_channel_done_desc)
            },
        )
    }

    private fun openFocus(): PendingIntent = PendingIntent.getActivity(
        context, 1,
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_FOCUS)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun action(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        context, requestCode, Intent(context, FocusService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun phaseLabel(state: FocusState): String = when (state.phase) {
        FocusKind.Focus -> context.getString(R.string.focus_phase_focus, state.session, state.sessionsPerCycle)
        FocusKind.ShortBreak -> context.getString(R.string.focus_phase_short_break)
        FocusKind.LongBreak -> context.getString(R.string.focus_phase_long_break)
    }

    /**
     * Notificação contínua do serviço. Correndo, o sistema desenha a contagem regressiva sozinho
     * (cronômetro até [FocusState.endsAt]), sem o app precisar atualizar a cada segundo.
     */
    fun ongoing(state: FocusState): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_stat_pulso)
            .setColor(ACCENT_VOLT) // foco = papel de tempo (volt)
            .setContentTitle(phaseLabel(state))
            .setContentIntent(openFocus())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        val task = state.taskTitle ?: context.getString(R.string.focus_no_task)
        val end = state.endsAt
        when {
            end != null -> builder
                .setContentText(task)
                .setWhen(end.toEpochMilli())
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, context.getString(R.string.focus_pause), action(FocusService.ACTION_PAUSE, 10))
            state.finished -> builder.setContentText(context.getString(R.string.focus_done_waiting)).setShowWhen(false)
            else -> builder
                .setContentText(context.getString(R.string.focus_paused_remaining, FocusMachine.format(state.remainingSeconds(Instant.now(clock)))))
                .setSubText(task)
                .setShowWhen(false)
                .addAction(0, context.getString(R.string.focus_resume), action(FocusService.ACTION_RESUME, 11))
        }
        return builder.addAction(0, context.getString(R.string.focus_stop), action(FocusService.ACTION_STOP, 12)).build()
    }

    /** Aviso sonoro de fim de fase ("Sessão concluída" / "Pausa concluída"). */
    fun phaseDone(state: FocusState) {
        val focus = state.phase == FocusKind.Focus
        val notification = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_pulso)
            .setColor(ACCENT_VOLT)
            .setContentTitle(context.getString(if (focus) R.string.focus_session_done else R.string.focus_break_done))
            .setContentText(state.taskTitle ?: context.getString(R.string.focus_done_waiting))
            .setContentIntent(openFocus())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        if (FocusController.hasNotificationPermission(context)) manager.notify(ID_DONE, notification)
    }

    fun cancelDone() = manager.cancel(ID_DONE)

    companion object {
        const val CHANNEL_TIMER = "focus_timer"
        const val CHANNEL_DONE = "focus_done"
        const val ID_ONGOING = 4201
        const val ID_DONE = 4202

        /** Volt do tema escuro (#2F95F5): a notificação é do sistema, fora do tema Compose. */
        const val ACCENT_VOLT = 0xFF2F95F5.toInt()
    }
}
