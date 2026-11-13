package com.organizador.agenda.ui.tasks

import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.task.TaskLists
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.domain.time.PtBr.capitalized
import com.organizador.agenda.ui.pulso.components.ChipTone
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ChipIcon { Flag, Clock, Calendar, Subtasks, Repeat }

data class TaskChipUi(val text: String, val tone: ChipTone = ChipTone.Neutral, val icon: ChipIcon? = null)

/** Uma linha de tarefa pronta para desenhar. */
data class TaskRowUi(
    val id: Long,
    val title: String,
    val done: Boolean,
    val priority: Priority,
    val chips: List<TaskChipUi>,
)

/** Converte tarefas em linhas de tela. Puro (testado em JVM). */
object TaskUiMapper {

    fun row(task: Task, now: LocalDateTime): TaskRowUi = TaskRowUi(
        id = task.id,
        title = task.title,
        done = task.isDone,
        priority = task.priority,
        chips = buildList {
            dueChip(task, now)?.let(::add)
            if (task.dueDate != null) task.recurrence?.let { add(TaskChipUi(PtBr.recurrence(it), icon = ChipIcon.Repeat)) }
            if (task.subtasks.isNotEmpty()) {
                add(TaskChipUi("${task.subtasks.count { it.done }}/${task.subtasks.size}", icon = ChipIcon.Subtasks))
            }
            task.tags.forEach { add(TaskChipUi("#${it.name}")) }
        },
    )

    /**
     * Chip de prazo. Papéis de cor: flare = atrasada; volt = horário de hoje (tempo/agora);
     * neutro = datas futuras. Tarefa de hoje sem horário não ganha chip (o grupo já diz "Hoje").
     */
    fun dueChip(task: Task, now: LocalDateTime): TaskChipUi? {
        val date = task.dueDate ?: return null
        val today = now.toLocalDate()
        val time = task.dueTime
        return when {
            TaskLists.isOverdue(task, now) -> TaskChipUi(
                text = "Atrasada · " + if (date == today) PtBr.time(time!!) else PtBr.relativeDay(date, today),
                tone = ChipTone.Flare,
                icon = ChipIcon.Flag,
            )
            date == today -> time?.let { TaskChipUi(PtBr.time(it), ChipTone.Volt, ChipIcon.Clock) }
            else -> TaskChipUi(whenLabel(date, time, today), icon = ChipIcon.Calendar)
        }
    }

    /** "Hoje", "Amanhã · 10:00", "Sex, 13 nov · 10:00". */
    fun whenLabel(date: LocalDate, time: LocalTime?, today: LocalDate): String =
        PtBr.relativeDay(date, today).capitalized() + (time?.let { " · ${PtBr.time(it)}" } ?: "")

    /** Para o botão de criar: "Hoje", "Amanhã, 09:00", "Ter 10, 14:00". */
    fun ctaWhen(date: LocalDate, time: LocalTime?, today: LocalDate): String {
        val day = when (date) {
            today -> "Hoje"
            today.plusDays(1) -> "Amanhã"
            else -> "${PtBr.weekdayShort(date.dayOfWeek).capitalized()} ${date.dayOfMonth}"
        }
        return day + (time?.let { ", ${PtBr.time(it)}" } ?: "")
    }
}
