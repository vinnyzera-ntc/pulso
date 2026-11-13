package com.organizador.agenda.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Prioridade P1 (mais alta) a P4 (sem prioridade, o padrão). */
enum class Priority(val level: Int) {
    P1(1), P2(2), P3(3), P4(4);

    companion object {
        fun of(level: Int): Priority = entries.firstOrNull { it.level == level } ?: P4
    }
}

data class Task(
    val id: Long = 0,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.P4,
    val dueDate: LocalDate? = null,
    /** Só tem sentido com [dueDate]. Nulo = tarefa do dia, sem horário. */
    val dueTime: LocalTime? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant = Instant.EPOCH,
    val subtasks: List<Subtask> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val reminders: List<TaskReminder> = emptyList(),
    /** Só tem sentido com [dueDate]. Concluir uma tarefa que se repete a leva para [nextDue]. */
    val recurrence: Recurrence? = null,
) {
    val isDone: Boolean get() = completedAt != null

    /**
     * Próximo vencimento ao concluir: a primeira ocorrência depois do vencimento atual ou de hoje
     * (o que for mais tarde), para uma tarefa atrasada não pular para outra data já vencida.
     * null = não se repete (concluir conclui de vez).
     */
    fun nextDue(today: LocalDate): LocalDate? {
        val rule = recurrence ?: return null
        val due = dueDate ?: return null
        return rule.nextAfter(maxOf(due, today), due)
    }

    /** Momento de vencimento: data + hora, ou só a data (fim do dia não importa aqui). */
    val dueDateTime: LocalDateTime? get() = dueDate?.let { d -> dueTime?.let { d.atTime(it) } }
}

data class Subtask(
    val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0,
)

data class Tag(val id: Long = 0, val name: String)

/**
 * Lembrete de tarefa, relativo ao vencimento: [offsetMinutes] antes de data+hora
 * (ou antes do horário padrão, para tarefas sem hora). [snoozedUntil] substitui o cálculo
 * quando o usuário adia pela notificação. [firedAt] evita disparar de novo após reiniciar.
 */
data class TaskReminder(
    val id: Long = 0,
    val offsetMinutes: Int = 0,
    val snoozedUntil: LocalDateTime? = null,
    val firedAt: Instant? = null,
) {
    /** Quando o lembrete deve tocar, ou null se a tarefa não tem data. */
    fun triggerAt(task: Task, defaultTime: LocalTime): LocalDateTime? {
        snoozedUntil?.let { return it }
        val date = task.dueDate ?: return null
        return date.atTime(task.dueTime ?: defaultTime).minusMinutes(offsetMinutes.toLong())
    }
}
