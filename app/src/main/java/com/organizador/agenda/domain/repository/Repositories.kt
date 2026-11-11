package com.organizador.agenda.domain.repository

import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.model.DeviceCalendar
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.model.EventDraft
import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.FocusSession
import com.organizador.agenda.domain.model.FocusSettings
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTask(id: Long): Flow<Task?>
    suspend fun getTask(id: Long): Task?

    /** Insere (id 0) ou atualiza a tarefa, substituindo subtarefas, etiquetas e lembretes. */
    suspend fun save(task: Task): Long
    suspend fun setCompleted(id: Long, completed: Boolean)

    /** Atualiza título, notas, prioridade e data/hora, sem tocar em subtarefas, etiquetas e lembretes. */
    suspend fun updateFields(task: Task)
    suspend fun setTags(taskId: Long, names: List<String>)

    /** Troca os lembretes pelos deslocamentos dados (minutos antes do vencimento). */
    suspend fun setReminders(taskId: Long, offsetsMinutes: List<Int>)

    /** Exclui e devolve a tarefa como estava, para "Desfazer". */
    suspend fun delete(id: Long): Task?

    /** Reinsere uma tarefa excluída, com o mesmo id. */
    suspend fun restore(task: Task)

    suspend fun addSubtask(taskId: Long, title: String): Long
    suspend fun setSubtaskDone(subtaskId: Long, done: Boolean)
    suspend fun renameSubtask(subtaskId: Long, title: String)
    suspend fun deleteSubtask(subtaskId: Long)

    fun observeTags(): Flow<List<Tag>>
}

interface FocusRepository {
    suspend fun start(taskId: Long?, kind: FocusKind, plannedMinutes: Int): Long
    suspend fun finish(sessionId: Long, completed: Boolean)
    fun observeForTask(taskId: Long): Flow<List<FocusSession>>
    fun observeBetween(from: Instant, to: Instant): Flow<List<FocusSession>>
}

interface CalendarRepository {
    fun hasPermission(): Boolean

    /** Pede nova leitura (ex.: logo após a permissão ser concedida). */
    fun refresh()

    /** Lista vazia quando não há permissão. Reemite quando o provider muda. */
    fun observeCalendars(): Flow<List<DeviceCalendar>>
    fun observeEvents(from: Instant, to: Instant, calendarIds: Set<Long>): Flow<List<CalendarEvent>>

    suspend fun getEvent(eventId: Long): EventDetails?
    suspend fun createEvent(draft: EventDraft): Long
    suspend fun updateEvent(eventId: Long, draft: EventDraft)
    suspend fun deleteEvent(eventId: Long)

    /** Recorrente, "só este": cancela a ocorrência que começa em [instanceStart]. */
    suspend fun cancelOccurrence(eventId: Long, instanceStart: Instant)

    /** Recorrente, "só este": altera só a ocorrência que começa em [instanceStart]. Devolve o id da exceção. */
    suspend fun updateOccurrence(eventId: Long, instanceStart: Instant, draft: EventDraft): Long

    /** Recorrente, "toda a série": aplica o rascunho (feito sobre a ocorrência de [instanceStart]) à série. */
    suspend fun updateSeries(eventId: Long, instanceStart: Instant, draft: EventDraft)
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setCalendarVisible(id: Long, visible: Boolean)
    suspend fun setDefaultCalendar(id: Long)
    suspend fun setFocus(focus: FocusSettings)
    suspend fun setTaskReminderTime(time: LocalTime)
    suspend fun setEventReminderMinutes(minutes: Int)
    suspend fun setWeekStart(day: DayOfWeek)
    suspend fun markPermissionRequested(permission: String)
}
