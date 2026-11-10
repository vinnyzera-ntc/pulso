package com.organizador.agenda.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.domain.calendar.CalendarSelection
import com.organizador.agenda.domain.calendar.NowNext
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.task.DayProgress
import com.organizador.agenda.domain.task.TaskLists
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.tasks.TaskRowUi
import com.organizador.agenda.ui.tasks.TaskUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** O cartão "Agora" (em andamento) ou "Próximo". */
data class EventCardUi(
    val eventId: Long,
    val live: Boolean,
    val timeRange: String,
    /** "termina em 43 min" ou "começa em 25 min". Sempre em volt. */
    val relative: String,
    val title: String,
    val location: String?,
    val progress: Float,
    val attendees: List<String> = emptyList(),
)

data class TodayUiState(
    val loaded: Boolean = false,
    val eyebrow: String = "",
    val greeting: String = "",
    val progress: DayProgress = DayProgress(0, 0),
    val calendarConnected: Boolean = true,
    val event: EventCardUi? = null,
    val tasks: List<TaskRowUi> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val calendar: CalendarRepository,
    settings: SettingsRepository,
    private val nowProvider: NowProvider,
) : ViewModel() {

    private val ticks = nowProvider.minuteTicks

    private val visibleCalendarIds: Flow<Set<Long>> =
        combine(calendar.observeCalendars(), settings.settings) { cals, s ->
            CalendarSelection.visibleIds(cals, s.calendarVisibility)
        }.distinctUntilChanged()

    /** Eventos de hoje; a consulta é refeita quando o dia vira ou as agendas visíveis mudam. */
    private val todayEvents: Flow<List<CalendarEvent>> =
        combine(ticks.map { it.toLocalDate() }.distinctUntilChanged(), visibleCalendarIds) { day, ids -> day to ids }
            .flatMapLatest { (day, ids) ->
                val zone = nowProvider.zone
                calendar.observeEvents(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant(), ids)
            }

    private val base: Flow<TodayUiState> = combine(ticks, tasks.observeTasks(), todayEvents) { now, all, events ->
        val todayTasks = TaskLists.today(all, now, nowProvider.zone)
        TodayUiState(
            loaded = true,
            eyebrow = PtBr.eyebrowDate(now.toLocalDate()),
            greeting = PtBr.greeting(now.toLocalTime()),
            progress = TaskLists.progress(todayTasks),
            calendarConnected = calendar.hasPermission(),
            event = eventCard(NowNext.pick(events, now.atZone(nowProvider.zone).toInstant())),
            tasks = todayTasks.map { TaskUiMapper.row(it, now) },
        )
    }

    /** Participantes só do evento em destaque (uma consulta, refeita quando o evento muda). */
    private val attendees: Flow<List<String>> = base.map { it.event?.eventId }.distinctUntilChanged()
        .mapLatest { id -> id?.let { calendar.getEvent(it)?.attendees?.map { a -> a.name } }.orEmpty() }

    val state: StateFlow<TodayUiState> =
        combine(base, attendees) { s, names -> s.copy(event = s.event?.copy(attendees = names)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun eventCard(pick: NowNext): EventCardUi? {
        fun range(e: CalendarEvent): String {
            val zone = nowProvider.zone
            fun t(i: java.time.Instant) = PtBr.time(LocalDateTime.ofInstant(i, zone).toLocalTime())
            return "${t(e.start)} – ${t(e.end)}"
        }
        return when (pick) {
            is NowNext.Current -> EventCardUi(
                pick.event.eventId, live = true, timeRange = range(pick.event),
                relative = "termina ${PtBr.inMinutes(pick.minutesLeft)}", title = pick.event.title,
                location = pick.event.location, progress = pick.progress,
            )
            is NowNext.Next -> EventCardUi(
                pick.event.eventId, live = false, timeRange = range(pick.event),
                relative = "começa ${PtBr.inMinutes(pick.minutesUntil)}", title = pick.event.title,
                location = pick.event.location, progress = 0f,
            )
            NowNext.None -> null
        }
    }

    fun setDone(id: Long, done: Boolean) = viewModelScope.launch { tasks.setCompleted(id, done) }

    /** Ao voltar para o app: a permissão de agenda pode ter mudado nas configurações. */
    fun onResume() = calendar.refresh()
}
