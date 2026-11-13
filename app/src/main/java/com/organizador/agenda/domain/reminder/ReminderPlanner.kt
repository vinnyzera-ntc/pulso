package com.organizador.agenda.domain.reminder

import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.model.TaskReminder
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/** Lembretes de uma tarefa que vencem agora: viram uma notificação só. */
data class DueTask(
    val task: Task,
    /** Lembretes que tocam agora (para marcar como disparados). */
    val reminderIds: List<Long>,
    /** O mais recente deles: é o que "Adiar" empurra para frente. */
    val latestReminderId: Long,
)

data class ReminderPlan(
    val due: List<DueTask>,
    /** Próximo disparo futuro (o único alarme armado), ou null se não há nenhum. */
    val next: Instant?,
)

/**
 * Decide o que notificar e quando acordar de novo. Regras:
 *  - tarefa concluída ou sem data não lembra;
 *  - um lembrete já disparado para o horário atual não dispara de novo ([TaskReminder.firedAt]
 *    posterior ao disparo); se a data da tarefa mudar para depois, ele volta a valer;
 *  - só dispara o que venceu depois de [since] (a última vez que o app conferiu): assim um
 *    lembrete perdido com o aparelho desligado toca ao religar, mas uma tarefa criada ou movida
 *    para um horário que já passou não notifica do nada.
 *
 * Horários são locais ([LocalTime] da tarefa) e convertidos com [zone] a cada cálculo: uma troca
 * de fuso só exige recalcular.
 */
object ReminderPlanner {
    fun plan(tasks: List<Task>, defaultTime: LocalTime, zone: ZoneId, since: Instant, now: Instant): ReminderPlan {
        val due = mutableListOf<DueTask>()
        var next: Instant? = null
        for (task in tasks) {
            if (task.isDone) continue
            val dueHere = mutableListOf<Pair<TaskReminder, Instant>>()
            for (r in task.reminders) {
                val at = r.triggerAt(task, defaultTime)?.atZone(zone)?.toInstant() ?: continue
                if (r.firedAt?.let { it >= at } == true) continue
                when {
                    at > now -> if (next == null || at < next) next = at
                    at > since -> dueHere += r to at
                }
            }
            if (dueHere.isNotEmpty()) {
                due += DueTask(task, dueHere.map { it.first.id }, dueHere.maxBy { it.second }.first.id)
            }
        }
        return ReminderPlan(due, next)
    }
}
