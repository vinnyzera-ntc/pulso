package com.organizador.agenda.ui.agenda

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.domain.calendar.AgendaMath
import com.organizador.agenda.domain.calendar.CalendarSelection
import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.model.DeviceCalendar
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.domain.time.PtBr.capitalized
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class AgendaMode { Day, Week, Month }

/** Um bloco da linha do tempo, já posicionado. */
data class EventBlockUi(
    val key: Long,
    val eventId: Long,
    val title: String,
    val location: String?,
    val timeRange: String,
    val startMin: Int,
    val endMin: Int,
    val column: Int,
    val columns: Int,
    /** Cor da agenda (dado do usuário no Google Agenda). */
    val color: Int,
    val live: Boolean,
    val canEdit: Boolean,
    val recurring: Boolean,
    val start: Instant,
    val end: Instant,
) {
    fun target(scope: EditScope) = EventTarget(eventId, key, start, end, scope)
}

data class AllDayUi(val key: Long, val eventId: Long, val title: String, val color: Int)

data class AgendaUiState(
    val loaded: Boolean = false,
    val hasAccess: Boolean = true,
    val permissionRequested: Boolean = false,
    val mode: AgendaMode = AgendaMode.Week,
    val selected: LocalDate = LocalDate.MIN,
    val today: LocalDate = LocalDate.MIN,
    /** Minutos desde a meia-noite, só quando o dia selecionado é hoje (linha do "agora"). */
    val nowMinutes: Int? = null,
    val weekDays: List<LocalDate> = emptyList(),
    val monthWeeks: List<List<LocalDate>> = emptyList(),
    val eventCounts: Map<LocalDate, Int> = emptyMap(),
    val allDay: List<AllDayUi> = emptyList(),
    val blocks: List<EventBlockUi> = emptyList(),
    val eyebrow: String = "",
    val title: String = "",
    val navigatorLabel: String = "",
)

private data class Selection(val mode: AgendaMode, val date: LocalDate)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val calendar: CalendarRepository,
    private val settings: SettingsRepository,
    private val nowProvider: NowProvider,
    private val editor: EventEditor,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val selection = MutableStateFlow(Selection(AgendaMode.Week, nowProvider.now().toLocalDate()))

    private data class Query(val from: LocalDate, val to: LocalDate, val ids: Set<Long>)

    private val calendars = calendar.observeCalendars()

    private val events = combine(selection, settings.settings, calendars) { sel, s, cals ->
        val (from, to) = span(sel.mode, sel.date, s.weekStart)
        Query(from, to, CalendarSelection.visibleIds(cals, s.calendarVisibility))
    }.distinctUntilChanged().flatMapLatest { q ->
        val zone = nowProvider.zone
        calendar.observeEvents(q.from.atStartOfDay(zone).toInstant(), q.to.plusDays(1).atStartOfDay(zone).toInstant(), q.ids)
    }

    val state: StateFlow<AgendaUiState> =
        combine(selection, settings.settings, calendars, events, nowProvider.minuteTicks) { sel, s, cals, evs, now ->
            buildState(sel.mode, sel.date, now, nowProvider.zone, s, cals, evs, calendar.hasPermission())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())

    fun setMode(mode: AgendaMode) = selection.update { it.copy(mode = mode) }
    fun select(date: LocalDate) = selection.update { it.copy(date = date) }
    fun goToday() = select(nowProvider.now().toLocalDate())

    /** Anterior/próximo: um dia, uma semana ou um mês, conforme a visão. */
    fun step(direction: Int) = selection.update {
        val d = direction.toLong()
        it.copy(
            date = when (it.mode) {
                AgendaMode.Day -> it.date.plusDays(d)
                AgendaMode.Week -> it.date.plusWeeks(d)
                AgendaMode.Month -> it.date.plusMonths(d)
            },
        )
    }

    fun onPermissionRequested() = viewModelScope.launch { settings.markPermissionRequested(Manifest.permission.READ_CALENDAR) }
    fun onResume() = calendar.refresh()

    /** Início (instante) de um horário do dia selecionado. */
    fun startAt(minutes: Int): LocalDateTime = selection.value.date.atStartOfDay().plusMinutes(minutes.toLong())

    fun move(block: EventBlockUi, newStartMin: Int, scope: EditScope) = appScope.launch {
        val newStart = startAt(newStartMin).atZone(nowProvider.zone).toInstant()
        editor.move(block.target(scope), newStart)
    }

    fun cancel(target: EventTarget) = editor.cancel(target)

    suspend fun details(eventId: Long): EventDetails? = calendar.getEvent(eventId)

    companion object {
        private fun span(mode: AgendaMode, date: LocalDate, weekStart: DayOfWeek): Pair<LocalDate, LocalDate> =
            if (mode == AgendaMode.Month) {
                val grid = AgendaMath.monthGrid(YearMonth.from(date), weekStart)
                grid.first().first() to grid.last().last()
            } else {
                val week = AgendaMath.weekOf(date, weekStart)
                week.first() to week.last()
            }

        /** Montagem do estado, pura (testada em JVM). */
        fun buildState(
            mode: AgendaMode,
            selected: LocalDate,
            now: LocalDateTime,
            zone: ZoneId,
            settings: AppSettings,
            calendars: List<DeviceCalendar>,
            events: List<CalendarEvent>,
            hasAccess: Boolean,
        ): AgendaUiState {
            val today = now.toLocalDate()
            val week = AgendaMath.weekOf(selected, settings.weekStart)
            val monthWeeks = AgendaMath.monthGrid(YearMonth.from(selected), settings.weekStart)
            val (from, to) = span(mode, selected, settings.weekStart)
            val counts = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }
                .associateWith { d -> events.count { AgendaMath.occursOn(it, d, zone) } }
                .filterValues { it > 0 }

            val day = AgendaMath.forDay(events, selected, zone)
            val placement = AgendaMath.layout(day.timed)
            val writable = calendars.filter { it.canWrite }.map { it.id }.toSet()
            val nowInstant = now.atZone(zone).toInstant()
            fun time(i: Instant) = PtBr.time(LocalDateTime.ofInstant(i, zone).toLocalTime())
            val blocks = day.timed.map { slot ->
                val e = slot.event
                val p = placement.getValue(e.instanceId)
                EventBlockUi(
                    key = e.instanceId, eventId = e.eventId, title = e.title.ifBlank { "(Sem título)" }, location = e.location,
                    timeRange = "${time(e.start)} – ${time(e.end)}",
                    startMin = slot.startMin, endMin = slot.endMin, column = p.column, columns = p.columns, color = e.color,
                    live = e.start <= nowInstant && nowInstant < e.end,
                    canEdit = e.calendarId in writable, recurring = e.isRecurring, start = e.start, end = e.end,
                )
            }
            val monthName = PtBr.month(selected.month).capitalized() + if (selected.year != today.year) " ${selected.year}" else ""
            return AgendaUiState(
                loaded = true,
                hasAccess = hasAccess,
                permissionRequested = Manifest.permission.READ_CALENDAR in settings.requestedPermissions,
                mode = mode,
                selected = selected,
                today = today,
                nowMinutes = if (selected == today) now.hour * 60 + now.minute else null,
                weekDays = week,
                monthWeeks = monthWeeks,
                eventCounts = counts,
                allDay = day.allDay.map { AllDayUi(it.instanceId, it.eventId, it.title.ifBlank { "(Sem título)" }, it.color) },
                blocks = blocks,
                eyebrow = "Semana ${AgendaMath.weekNumber(selected)}",
                title = monthName,
                navigatorLabel = when (mode) {
                    AgendaMode.Day -> PtBr.shortDate(selected)
                    AgendaMode.Week -> weekLabel(week)
                    AgendaMode.Month -> "${PtBr.month(selected.month).capitalized()} ${selected.year}"
                },
            )
        }

        /** "8 – 14 nov" ou "29 nov – 5 dez". */
        fun weekLabel(week: List<LocalDate>): String {
            val a = week.first()
            val b = week.last()
            return if (a.month == b.month) "${a.dayOfMonth} – ${b.dayOfMonth} ${PtBr.monthShort(b.month)}"
            else "${a.dayOfMonth} ${PtBr.monthShort(a.month)} – ${b.dayOfMonth} ${PtBr.monthShort(b.month)}"
        }
    }
}
