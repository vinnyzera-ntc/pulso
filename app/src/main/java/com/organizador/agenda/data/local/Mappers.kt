package com.organizador.agenda.data.local

import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.FocusSession
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Subtask
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.model.TaskReminder
import java.time.Instant
import java.util.Locale

fun TaskWithDetails.toDomain(): Task = Task(
    id = task.id,
    title = task.title,
    notes = task.notes,
    priority = Priority.of(task.priority),
    dueDate = task.dueDate,
    dueTime = task.dueTime,
    completedAt = task.completedAt,
    createdAt = task.createdAt,
    subtasks = subtasks.sortedBy { it.position }.map { Subtask(it.id, it.title, it.done, it.position) },
    tags = tags.sortedBy { it.name }.map { Tag(it.id, it.name) },
    reminders = reminders.sortedBy { it.offsetMinutes }.map {
        TaskReminder(it.id, it.offsetMinutes, it.snoozedUntil, it.firedAt)
    },
)

fun Task.toEntity(now: Instant): TaskEntity = TaskEntity(
    id = id,
    title = title.trim(),
    notes = notes,
    priority = priority.level,
    dueDate = dueDate,
    dueTime = if (dueDate == null) null else dueTime,
    completedAt = completedAt,
    createdAt = if (id == 0L) now else createdAt,
    updatedAt = now,
)

/** "#Pessoal " -> "pessoal". */
fun normalizeTagName(raw: String): String = raw.trim().removePrefix("#").trim().lowercase(Locale.ROOT)

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    taskId = taskId,
    kind = runCatching { FocusKind.valueOf(kind) }.getOrDefault(FocusKind.Focus),
    plannedMinutes = plannedMinutes,
    startedAt = startedAt,
    endedAt = endedAt,
    completed = completed,
)
