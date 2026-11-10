package com.organizador.agenda.domain.task

import com.organizador.agenda.domain.model.Task
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Filtro da tela Tarefas. */
sealed interface TaskFilter {
    data object All : TaskFilter
    data object Today : TaskFilter
    data object Next7Days : TaskFilter
    data object Overdue : TaskFilter
    data object NoDate : TaskFilter
    data class ByTag(val tagId: Long) : TaskFilter
}

/** Grupos da tela Tarefas, na ordem em que aparecem. */
enum class TaskSection { Overdue, Today, Tomorrow, Next7Days, Later, NoDate }

data class TaskGroups(
    val sections: List<Pair<TaskSection, List<Task>>>,
    /** Concluídas que passam pelo filtro, da mais recente para a mais antiga. */
    val completed: List<Task>,
)

data class DayProgress(val done: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else done * 100 / total
}

/** Regras de listas de tarefas: o que é atrasado, o que entra no Hoje, ordenação e grupos. Puras. */
object TaskLists {

    /** Aberta e vencida: data anterior a hoje, ou hoje com horário que já passou. */
    fun isOverdue(task: Task, now: LocalDateTime): Boolean {
        if (task.isDone) return false
        val date = task.dueDate ?: return false
        val today = now.toLocalDate()
        return date < today || (date == today && task.dueTime != null && task.dueTime < now.toLocalTime())
    }

    /** Por data; no mesmo dia, com horário antes (por hora) e depois sem horário; então prioridade. */
    val openOrder: Comparator<Task> = compareBy(
        { it.dueDate ?: LocalDate.MAX },
        { it.dueTime == null },
        { it.dueTime },
        { it.priority.level },
        { it.id },
    )

    private fun completedDate(task: Task, zone: ZoneId): LocalDate? = task.completedAt?.atZone(zone)?.toLocalDate()

    /**
     * Tarefas do painel Hoje: abertas com data até hoje (atrasadas primeiro, depois por hora e
     * prioridade) e, no fim, as concluídas hoje.
     */
    fun today(tasks: List<Task>, now: LocalDateTime, zone: ZoneId): List<Task> {
        val today = now.toLocalDate()
        val open = tasks.filter { !it.isDone && it.dueDate != null && it.dueDate <= today }.sortedWith(openOrder)
        val done = tasks.filter { completedDate(it, zone) == today }.sortedBy { it.completedAt }
        return open + done
    }

    fun progress(todayTasks: List<Task>): DayProgress = DayProgress(todayTasks.count { it.isDone }, todayTasks.size)

    fun overdueCount(tasks: List<Task>, now: LocalDateTime): Int = tasks.count { isOverdue(it, now) }

    private fun sectionOf(task: Task, today: LocalDate): TaskSection {
        val date = task.dueDate ?: return TaskSection.NoDate
        return when {
            date < today -> TaskSection.Overdue
            date == today -> TaskSection.Today
            date == today.plusDays(1) -> TaskSection.Tomorrow
            date <= today.plusDays(7) -> TaskSection.Next7Days
            else -> TaskSection.Later
        }
    }

    private fun matches(task: Task, filter: TaskFilter, now: LocalDateTime): Boolean {
        val today = now.toLocalDate()
        val date = task.dueDate
        return when (filter) {
            TaskFilter.All -> true
            TaskFilter.Today -> date == today
            TaskFilter.Next7Days -> date != null && date >= today && date <= today.plusDays(7)
            // Concluídas nunca são "atrasadas", então esse filtro não mostra concluídas.
            TaskFilter.Overdue -> isOverdue(task, now)
            TaskFilter.NoDate -> date == null
            is TaskFilter.ByTag -> task.tags.any { it.id == filter.tagId }
        }
    }

    fun group(tasks: List<Task>, filter: TaskFilter, now: LocalDateTime): TaskGroups {
        val today = now.toLocalDate()
        val matching = tasks.filter { matches(it, filter, now) }
        val sections = matching.filter { !it.isDone }
            .groupBy { sectionOf(it, today) }
            .toSortedMap()
            .map { (section, list) -> section to list.sortedWith(openOrder) }
        val completed = matching.filter { it.isDone }.sortedByDescending { it.completedAt }
        return TaskGroups(sections, completed)
    }
}
