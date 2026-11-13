package com.organizador.agenda.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.domain.reminder.ReminderPlanner
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lembretes de tarefas. Arma um único alarme exato, para o próximo lembrete; quando ele toca
 * (ou a lista de tarefas muda, o aparelho religa, a hora ou o fuso mudam), [refresh] confere
 * tudo de novo: notifica o que venceu, marca como disparado e arma o próximo.
 *
 * "Última conferência" ([KEY_SINCE]) separa um lembrete perdido (aparelho desligado: toca ao
 * religar) de um horário que já tinha passado quando a tarefa foi criada (não toca). Fica fora
 * do backup, para uma restauração não disparar lembretes antigos.
 */
@Singleton
class TaskReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val settings: SettingsRepository,
    private val notifier: ReminderNotifier,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var started = false

    /** Chamado uma vez ao abrir o processo: acompanha as mudanças nas tarefas e no horário padrão. */
    fun start() {
        if (started) return
        started = true
        scope.launch {
            combine(tasks.observeTasks(), settings.settings.map { it.taskReminderTime }.distinctUntilChanged()) { _, _ -> }
                .conflate()
                .collect { refresh() }
        }
    }

    suspend fun refresh() = mutex.withLock {
        val now = Instant.now(clock)
        // Se o relógio voltou, a última conferência "no futuro" não pode esconder lembretes.
        val since = prefs.getLong(KEY_SINCE, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)?.coerceAtMost(now) ?: now
        val all = tasks.observeTasks().first()
        val defaultTime = settings.settings.first().taskReminderTime
        val plan = ReminderPlanner.plan(all, defaultTime, clock.zone, since, now)

        plan.due.forEach { notifier.show(it, LocalDateTime.ofInstant(now, clock.zone)) }
        tasks.markRemindersFired(plan.due.flatMap { it.reminderIds }, now)
        prefs.edit().putLong(KEY_SINCE, now.toEpochMilli()).apply()
        // Concluída ou excluída em outro lugar: a notificação some.
        notifier.cancelAllExcept(all.filter { !it.isDone }.map { it.id }.toSet())
        arm(plan.next)
    }

    private fun arm(at: Instant?) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, REQUEST_ALARM,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_FIRE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (at == null) {
            alarms.cancel(intent)
            return
        }
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
    }

    companion object {
        /** Arquivo excluído do backup (res/xml/backup_rules.xml e data_extraction_rules.xml). */
        const val PREFS = "reminders"
        private const val KEY_SINCE = "since"
        private const val REQUEST_ALARM = 300
    }
}
