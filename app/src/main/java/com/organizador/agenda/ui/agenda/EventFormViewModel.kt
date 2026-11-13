package com.organizador.agenda.ui.agenda

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.domain.calendar.CalendarSelection
import com.organizador.agenda.domain.calendar.EventForm
import com.organizador.agenda.domain.calendar.EventSummary
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.time.NowProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

data class CalendarOption(val id: Long, val name: String, val account: String, val color: Int)

/** O que a confirmação mostra depois de gravar. */
data class EventConfirmation(
    val created: Boolean,
    /** "Ter, 10 nov · 14:00" */
    val value: String,
    /** "45 min · Sala 3" */
    val meta: String,
    val reminder: String?,
)

data class EventFormUiState(
    val ready: Boolean = false,
    val hasCalendarAccess: Boolean = true,
    val calendarPermissionRequested: Boolean = false,
    val editing: Boolean = false,
    val form: EventForm? = null,
    val today: LocalDate = LocalDate.MIN,
    val calendars: List<CalendarOption> = emptyList(),
    val saving: Boolean = false,
    val confirmation: EventConfirmation? = null,
) {
    val calendar: CalendarOption? get() = calendars.firstOrNull { it.id == form?.calendarId }
}

@HiltViewModel
class EventFormViewModel @Inject constructor(
    private val calendar: CalendarRepository,
    private val settings: SettingsRepository,
    private val nowProvider: NowProvider,
    private val editor: EventEditor,
) : ViewModel() {

    private val _state = MutableStateFlow(EventFormUiState())
    val state: StateFlow<EventFormUiState> = _state

    private var target: EventTarget? = null
    private var requestedStart: LocalDateTime? = null

    /**
     * Prepara a folha: [editTarget] nulo = compromisso novo começando em [start] (ou na próxima
     * meia hora). Chamado a cada abertura, porque o ViewModel é reaproveitado entre aberturas.
     */
    fun start(editTarget: EventTarget?, start: LocalDateTime?) = viewModelScope.launch {
        target = editTarget
        requestedStart = start
        val s = settings.settings.first()
        val now = nowProvider.now()
        val base = EventFormUiState(
            today = now.toLocalDate(),
            hasCalendarAccess = calendar.hasPermission(),
            calendarPermissionRequested = Manifest.permission.READ_CALENDAR in s.requestedPermissions,
            editing = editTarget != null,
        )
        if (!base.hasCalendarAccess) {
            _state.value = base.copy(ready = true)
            return@launch
        }
        _state.value = base
        val all = calendar.observeCalendars().first()
        val options = all.filter { it.canWrite }.map { CalendarOption(it.id, it.displayName, it.accountName, it.color) }
        val form = if (editTarget == null) {
            EventForm.new(
                start = start ?: EventForm.suggestedStart(now),
                calendarId = CalendarSelection.defaultForCreate(all, s.defaultCalendarId)?.id,
                reminderMinutes = s.eventReminderMinutes,
            )
        } else {
            calendar.getEvent(editTarget.eventId)?.let { EventForm.edit(it, editTarget.instanceStart, editTarget.instanceEnd, nowProvider.zone) }
        }
        _state.value = base.copy(ready = true, form = form, calendars = options)
    }

    fun update(transform: (EventForm) -> EventForm) = _state.update { it.copy(form = it.form?.let(transform)) }

    fun save() {
        val current = _state.value
        val form = current.form?.takeIf { it.isValid } ?: return
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (!editor.save(target, form.toDraft(nowProvider.zone))) {
                // Falhou (o aviso já foi mostrado): a folha continua aberta para tentar de novo.
                _state.update { it.copy(saving = false) }
                return@launch
            }
            _state.update {
                it.copy(
                    saving = false,
                    confirmation = EventConfirmation(
                        created = target == null,
                        value = EventSummary.value(form),
                        meta = EventSummary.meta(form),
                        reminder = EventSummary.reminder(form),
                    ),
                )
            }
        }
    }

    fun onCalendarPermissionRequested() = viewModelScope.launch {
        settings.markPermissionRequested(Manifest.permission.READ_CALENDAR)
        _state.update { it.copy(calendarPermissionRequested = true) }
    }

    fun onCalendarPermissionResult() {
        calendar.refresh()
        start(target, requestedStart)
    }
}
