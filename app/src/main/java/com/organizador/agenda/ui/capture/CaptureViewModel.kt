package com.organizador.agenda.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.R
import com.organizador.agenda.domain.calendar.CalendarSelection
import com.organizador.agenda.domain.capture.CaptureKind
import com.organizador.agenda.domain.capture.CaptureParser
import com.organizador.agenda.domain.capture.ParsedCapture
import com.organizador.agenda.domain.model.EventDraft
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.common.UiMessage
import com.organizador.agenda.ui.pulso.components.ChipTone
import com.organizador.agenda.ui.tasks.ChipIcon
import com.organizador.agenda.ui.tasks.TaskChipUi
import com.organizador.agenda.ui.tasks.TaskUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class CaptureUiState(
    val text: String = "",
    val kind: CaptureKind = CaptureKind.Task,
    /** Dá para alternar entre tarefa e compromisso (exige horário e uma agenda com escrita). */
    val canToggleKind: Boolean = false,
    /** Trechos reconhecidos, para o destaque do campo. */
    val highlights: List<IntRange> = emptyList(),
    /** "Entendi assim": data/hora em volt, o resto neutro. */
    val chips: List<TaskChipUi> = emptyList(),
    /** A frase tem repetição ("toda segunda"), que ainda não é suportada. */
    /** Compromisso com repetição: ela fica de fora (o compromisso é criado uma vez). */
    val recurrenceIgnored: Boolean = false,
    val canSend: Boolean = false,
    val recents: List<String> = emptyList(),
)

private data class Input(val text: String = "", val kindOverride: CaptureKind? = null)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val calendar: CalendarRepository,
    private val settings: SettingsRepository,
    private val nowProvider: NowProvider,
    private val messenger: Messenger,
) : ViewModel() {

    private val input = MutableStateFlow(Input())

    /** Agenda onde um compromisso capturado é criado; null se não há permissão ou agenda com escrita. */
    private val targetCalendar = combine(calendar.observeCalendars(), settings.settings) { cals, s ->
        if (calendar.hasPermission()) CalendarSelection.defaultForCreate(cals, s.defaultCalendarId)?.id else null
    }

    val state: StateFlow<CaptureUiState> =
        combine(input, targetCalendar, settings.settings, nowProvider.minuteTicks) { i, calendarId, s, now ->
            buildState(i.text, i.kindOverride, CaptureParser.parse(i.text, now), now, calendarId != null, s.recentCaptures)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureUiState())

    fun setText(text: String) = input.update { it.copy(text = text) }

    fun toggleKind() = input.update {
        val current = state.value.kind
        it.copy(kindOverride = if (current == CaptureKind.Task) CaptureKind.Event else CaptureKind.Task)
    }

    fun send() = viewModelScope.launch {
        val current = state.value
        if (!current.canSend) return@launch
        val text = current.text
        val parsed = CaptureParser.parse(text, nowProvider.now())
        val sent = if (current.kind == CaptureKind.Event) createEvent(parsed) else { createTask(parsed); true }
        // Falhou ao gravar na agenda: o texto fica no campo para tentar de novo.
        if (!sent) return@launch
        settings.addRecentCapture(text)
        input.value = Input()
    }

    private suspend fun createTask(p: ParsedCapture) {
        val id = tasks.save(
            Task(
                title = p.title, priority = p.priority, dueDate = p.date, dueTime = p.time,
                tags = p.tags.map { Tag(name = it) }, recurrence = p.recurrence,
            ),
        )
        messenger.show(UiMessage(R.string.msg_task_created, R.string.action_undo) { tasks.delete(id) })
    }

    /** false = não gravou (sem agenda ou erro do provider, já avisado). */
    private suspend fun createEvent(p: ParsedCapture): Boolean {
        val s = settings.settings.first()
        val calendarId = CalendarSelection.defaultForCreate(calendar.observeCalendars().first(), s.defaultCalendarId)?.id
        if (calendarId == null) {
            messenger.show(UiMessage(R.string.msg_no_calendar))
            return false
        }
        val zone = nowProvider.zone
        val start = LocalDateTime.of(p.date, p.time).atZone(zone).toInstant()
        val id = messenger.guard(R.string.msg_calendar_write_failed) { calendar.createEvent(
            EventDraft(
                calendarId = calendarId,
                title = p.title,
                start = start,
                end = start.plusSeconds((p.durationMinutes ?: DEFAULT_EVENT_MINUTES) * 60L),
                timeZone = zone,
                reminderMinutes = listOf(s.eventReminderMinutes),
            ),
        ) } ?: return false
        val label = "${PtBr.shortDate(p.date!!)} · ${PtBr.time(p.time!!)}"
        messenger.show(UiMessage(R.string.msg_event_created, R.string.action_undo, args = listOf(label)) { calendar.deleteEvent(id) })
        return true
    }

    companion object {
        /** Duração de um compromisso capturado sem "por 1h" nem "das 14h às 15h". */
        const val DEFAULT_EVENT_MINUTES = 60

        /** Montagem do estado, pura (testada em JVM). */
        fun buildState(
            text: String,
            kindOverride: CaptureKind?,
            parsed: ParsedCapture,
            now: LocalDateTime,
            hasWritableCalendar: Boolean,
            recents: List<String>,
        ): CaptureUiState {
            // Compromisso precisa de horário e de uma agenda onde criar; senão, só tarefa.
            val canBeEvent = parsed.time != null && hasWritableCalendar
            val kind = if (canBeEvent) kindOverride ?: parsed.kind else CaptureKind.Task
            val chips = buildList {
                val date = parsed.date
                if (date != null) {
                    add(
                        TaskChipUi(
                            TaskUiMapper.whenLabel(date, parsed.time, now.toLocalDate()),
                            ChipTone.Volt,
                            if (parsed.time != null) ChipIcon.Clock else ChipIcon.Calendar,
                        ),
                    )
                }
                if (kind == CaptureKind.Event) {
                    add(TaskChipUi(PtBr.duration(parsed.durationMinutes ?: DEFAULT_EVENT_MINUTES)))
                } else {
                    parsed.recurrence?.let { add(TaskChipUi(PtBr.recurrence(it), ChipTone.Volt, ChipIcon.Repeat)) }
                    if (parsed.priority != Priority.P4) add(TaskChipUi(parsed.priority.name, icon = ChipIcon.Flag))
                    parsed.tags.forEach { add(TaskChipUi("#$it")) }
                }
            }
            return CaptureUiState(
                text = text,
                kind = kind,
                canToggleKind = canBeEvent,
                highlights = parsed.spans.map { it.range },
                chips = if (parsed.isEmpty) emptyList() else chips,
                recurrenceIgnored = kind == CaptureKind.Event && parsed.recurrence != null,
                canSend = !parsed.isEmpty,
                recents = recents,
            )
        }
    }
}
