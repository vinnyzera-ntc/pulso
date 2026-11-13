package com.organizador.agenda.widget

import com.organizador.agenda.domain.calendar.CalendarSelection
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Fonte de dados do widget: as mesmas tarefas e compromissos do Hoje. */
@Singleton
class WidgetData @Inject constructor(
    private val tasks: TaskRepository,
    private val calendar: CalendarRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    /** Reemite quando tarefas, agendas visíveis ou compromissos de hoje mudam. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun states(): Flow<WidgetState> {
        val visible = combine(calendar.observeCalendars(), settings.settings) { cals, s -> CalendarSelection.visibleIds(cals, s.calendarVisibility) }
            .distinctUntilChanged()
        // A consulta de compromissos é refeita quando o dia vira.
        val events = combine(days(), visible) { day, ids -> day to ids }.flatMapLatest { (day, ids) ->
            val zone = clock.zone
            calendar.observeEvents(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant(), ids)
        }
        return combine(tasks.observeTasks(), events) { all, todayEvents ->
            WidgetModel.build(all, todayEvents, LocalDateTime.now(clock), clock.zone)
        }.distinctUntilChanged()
    }

    /** Emite hoje e, a cada meia-noite, o novo dia. */
    private fun days(): Flow<LocalDate> = flow {
        while (true) {
            val now = LocalDateTime.now(clock)
            emit(now.toLocalDate())
            delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000)
        }
    }

    suspend fun current(): WidgetState = states().first()

    suspend fun complete(taskId: Long) {
        tasks.setCompleted(taskId, true)
    }
}
