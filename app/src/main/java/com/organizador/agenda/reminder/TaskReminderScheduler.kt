package com.organizador.agenda.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.organizador.agenda.alarm.setWakeUpAt
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.domain.reminder.ReminderPlanner
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lembretes de tarefas. Arma um único alarme exato, para o próximo lembrete; quando ele toca
 * (ou a lista de tarefas muda, o aparelho religa, a hora ou o fuso mudam), [refresh] confere
 * tudo de novo: notifica o que venceu, marca como disparado e arma o próximo.
 *
 * "Última conferência" ([KEY_SINCE]) separa um lembrete perdido (aparelho desligado: toca ao
 * religar) de um horário que já tinha passado quando a tarefa foi criada (não toca). Fica fora
 * do backup, para uma restauração não disparar lembretes antigos.
 *
 * Conferências causadas por edição (o usuário criou ou mudou uma tarefa) só olham o último
 * minuto: o que venceu antes disso já tocou pelo alarme, ou já tinha passado ao ser criado.
 * A primeira conferência do processo e as dos receivers (alarme, religar, hora/fuso) usam a
 * última conferência gravada, para recuperar o que se perdeu.
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
            var first = true
            combine(tasks.observeTasks(), settings.settings.map { it.taskReminderTime }.distinctUntilChanged()) { _, _ -> }
                .conflate()
                .collect {
                    refresh(fromEdit = !first)
                    first = false
                }
        }
    }

    suspend fun refresh(fromEdit: Boolean = false) = mutex.withLock {
        try {
            check(fromEdit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Um erro aqui (banco, notificação, alarme) não pode derrubar o app nem parar a coleta.
            Log.w("Pulso", "Falha ao conferir lembretes", e)
        }
    }

    private suspend fun check(fromEdit: Boolean) {
        val now = Instant.now(clock)
        val stored = prefs.getLong(KEY_SINCE, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
        val since = ReminderPlanner.since(stored, now, fromEdit)
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
        alarms.setWakeUpAt(at.toEpochMilli(), intent)
    }

    companion object {
        /** Arquivo excluído do backup (res/xml/backup_rules.xml e data_extraction_rules.xml). */
        const val PREFS = "reminders"
        private const val KEY_SINCE = "since"
        private const val REQUEST_ALARM = 300
    }
}
