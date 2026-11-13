package com.organizador.agenda.testing

import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.model.DeviceCalendar
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.model.EventDraft
import com.organizador.agenda.domain.model.FocusSettings
import com.organizador.agenda.domain.model.Subtask
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.model.TaskReminder
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.time.NowProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

val TEST_ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")

class FakeNow(initial: LocalDateTime) : NowProvider {
    val current = MutableStateFlow(initial)
    override val zone: ZoneId = TEST_ZONE
    override fun now(): LocalDateTime = current.value
    override val minuteTicks: Flow<LocalDateTime> = current
    fun instant(): Instant = current.value.atZone(zone).toInstant()
}

/** Repositório de tarefas em memória, com as mesmas regras visíveis do Room (ids, etiquetas por nome). */
class FakeTaskRepository(private val now: () -> Instant = { Instant.EPOCH }) : TaskRepository {
    val state = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L
    private val tagIds = mutableMapOf<String, Long>()

    private fun tag(name: String) = Tag(tagIds.getOrPut(name) { tagIds.size + 1L }, name)
    private fun mutate(id: Long, f: (Task) -> Task) = state.update { list -> list.map { if (it.id == id) f(it) else it } }

    override fun observeTasks(): Flow<List<Task>> = state
    override fun observeTask(id: Long): Flow<Task?> = state.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun getTask(id: Long): Task? = state.value.firstOrNull { it.id == id }

    override suspend fun save(task: Task): Long {
        val id = if (task.id == 0L) nextId++ else task.id
        val stored = task.copy(
            id = id,
            tags = task.tags.map { tag(it.name) },
            subtasks = task.subtasks.mapIndexed { i, s -> s.copy(id = id * 100 + i, position = i) },
        )
        state.update { list -> list.filter { it.id != id } + stored }
        return id
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) = mutate(id) { it.copy(completedAt = if (completed) now() else null) }
    override suspend fun updateFields(task: Task) =
        mutate(task.id) { it.copy(title = task.title, notes = task.notes, priority = task.priority, dueDate = task.dueDate, dueTime = task.dueTime) }
    override suspend fun setTags(taskId: Long, names: List<String>) = mutate(taskId) { it.copy(tags = names.map(::tag)) }
    override suspend fun setReminders(taskId: Long, offsetsMinutes: List<Int>) =
        mutate(taskId) { it.copy(reminders = offsetsMinutes.map { m -> TaskReminder(offsetMinutes = m) }) }

    override suspend fun markRemindersFired(ids: List<Long>, at: Instant) = state.update { list ->
        list.map { t -> t.copy(reminders = t.reminders.map { if (it.id in ids) it.copy(firedAt = at) else it }) }
    }
    override suspend fun snoozeReminder(reminderId: Long, until: java.time.LocalDateTime) = state.update { list ->
        list.map { t -> t.copy(reminders = t.reminders.map { if (it.id == reminderId) it.copy(snoozedUntil = until) else it }) }
    }

    override suspend fun delete(id: Long): Task? {
        val snapshot = getTask(id) ?: return null
        state.update { list -> list.filter { it.id != id } }
        return snapshot
    }

    override suspend fun restore(task: Task) = state.update { it + task }

    override suspend fun addSubtask(taskId: Long, title: String): Long {
        val subId = nextId++ * 1000
        mutate(taskId) { it.copy(subtasks = it.subtasks + Subtask(subId, title.trim(), position = it.subtasks.size)) }
        return subId
    }

    private fun mutateSub(subId: Long, f: (Subtask) -> Subtask?) =
        state.update { list -> list.map { t -> t.copy(subtasks = t.subtasks.mapNotNull { if (it.id == subId) f(it) else it }) } }

    override suspend fun setSubtaskDone(subtaskId: Long, done: Boolean) = mutateSub(subtaskId) { it.copy(done = done) }
    override suspend fun renameSubtask(subtaskId: Long, title: String) = mutateSub(subtaskId) { it.copy(title = title.trim()) }
    override suspend fun deleteSubtask(subtaskId: Long) = mutateSub(subtaskId) { null }

    override fun observeTags(): Flow<List<Tag>> = state.map { list -> list.flatMap { it.tags }.distinct().sortedBy { it.name } }
}

class FakeSettingsRepository : SettingsRepository {
    val state = MutableStateFlow(AppSettings())
    override val settings: Flow<AppSettings> = state
    override suspend fun setCalendarVisible(id: Long, visible: Boolean) =
        state.update { it.copy(calendarVisibility = it.calendarVisibility + (id to visible)) }
    override suspend fun setDefaultCalendar(id: Long) = state.update { it.copy(defaultCalendarId = id) }
    override suspend fun setFocus(focus: FocusSettings) = state.update { it.copy(focus = focus) }
    override suspend fun setTaskReminderTime(time: LocalTime) = state.update { it.copy(taskReminderTime = time) }
    override suspend fun setEventReminderMinutes(minutes: Int) = state.update { it.copy(eventReminderMinutes = minutes) }
    override suspend fun setWeekStart(day: DayOfWeek) = state.update { it.copy(weekStart = day) }
    override suspend fun markPermissionRequested(permission: String) =
        state.update { it.copy(requestedPermissions = it.requestedPermissions + permission) }
    override suspend fun clearRecentCaptures() = state.update { it.copy(recentCaptures = emptyList()) }
override suspend fun addRecentCapture(text: String) =
        state.update { it.copy(recentCaptures = (listOf(text) + it.recentCaptures.filter { c -> c != text }).take(5)) }
}

class FakeCalendarRepository(
    calendars: List<DeviceCalendar> = emptyList(),
    events: List<CalendarEvent> = emptyList(),
    var permission: Boolean = true,
) : CalendarRepository {
    val calendars = MutableStateFlow(calendars)
    val events = MutableStateFlow(events)
    var details: Map<Long, EventDetails> = emptyMap()

    override fun hasPermission() = permission
    override fun refresh() = Unit
    override fun observeCalendars(): Flow<List<DeviceCalendar>> = calendars
    override fun observeEvents(from: Instant, to: Instant, calendarIds: Set<Long>): Flow<List<CalendarEvent>> =
        events.map { list -> list.filter { it.calendarId in calendarIds && it.end > from && it.start < to } }
    override suspend fun getEvent(eventId: Long): EventDetails? = details[eventId]
    /** Registro das escritas, na ordem: "create", "update:5", "delete:5", "cancelOccurrence:5", ... */
    val writes = mutableListOf<String>()
    var lastDraft: EventDraft? = null

    override suspend fun createEvent(draft: EventDraft): Long {
        writes += "create"; lastDraft = draft
        return 100
    }
    override suspend fun updateEvent(eventId: Long, draft: EventDraft) { writes += "update:$eventId"; lastDraft = draft }
    override suspend fun deleteEvent(eventId: Long) { writes += "delete:$eventId" }
    override suspend fun cancelOccurrence(eventId: Long, instanceStart: Instant) { writes += "cancelOccurrence:$eventId" }
    override suspend fun updateOccurrence(eventId: Long, instanceStart: Instant, draft: EventDraft): Long {
        writes += "updateOccurrence:$eventId"; lastDraft = draft
        return 200
    }
    override suspend fun updateSeries(eventId: Long, instanceStart: Instant, draft: EventDraft) { writes += "updateSeries:$eventId"; lastDraft = draft }
}

fun deviceCalendar(id: Long, google: Boolean = true, primary: Boolean = false, visible: Boolean = true, write: Boolean = true, name: String = "C$id", account: String = "eu@gmail.com") =
    DeviceCalendar(id, name, account, if (google) DeviceCalendar.GOOGLE_ACCOUNT_TYPE else "local", id.toInt(), primary, visible, write)

fun calendarEvent(id: Long, start: Instant, end: Instant, title: String = "Evento $id", calendarId: Long = 1, allDay: Boolean = false, location: String? = null) =
    CalendarEvent(id, id, calendarId, title, start, end, allDay, location, 0, false)
