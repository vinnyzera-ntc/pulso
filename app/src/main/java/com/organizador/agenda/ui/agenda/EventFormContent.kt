package com.organizador.agenda.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.domain.calendar.EventForm
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.CalendarChoice
import com.organizador.agenda.ui.common.CalendarChoiceSheet
import com.organizador.agenda.ui.common.CalendarDot
import com.organizador.agenda.ui.common.FieldPanel
import com.organizador.agenda.ui.common.PermissionPanel
import com.organizador.agenda.ui.common.PermissionRequest
import com.organizador.agenda.ui.common.PulsoDatePickerDialog
import com.organizador.agenda.ui.common.PulsoTextField
import com.organizador.agenda.ui.common.PulsoTimePickerDialog
import com.organizador.agenda.ui.common.calendarPermissionCopy
import com.organizador.agenda.ui.common.useTimeLabel
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.components.BigNumberField
import com.organizador.agenda.ui.pulso.components.ConfirmationContent
import com.organizador.agenda.ui.pulso.components.LabelIcon
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.PulsoCard
import com.organizador.agenda.ui.pulso.components.QuickChips
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.SurfaceLevel
import com.organizador.agenda.ui.tasks.FieldLabel
import com.organizador.agenda.ui.tasks.TaskUiMapper

/**
 * Conteúdo da folha de compromisso (padrão "Novo item"): título; painel de duração com número
 * grande, atalhos e a agenda de destino; data | hora; local; lembrete; e o botão que diz o
 * resultado ("Agendar · Ter 10, 14:00"). Depois de gravar, a confirmação substitui o formulário.
 */
@Composable
fun ColumnScope.EventFormContent(
    state: EventFormUiState,
    calendarPermission: PermissionRequest,
    onChange: ((EventForm) -> EventForm) -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
) {
    val c = Pulso.colors
    state.confirmation?.let { confirmation ->
        EventConfirmationContent(confirmation, onDone)
        return
    }
    SectionLabel(
        stringResource(if (state.editing) R.string.event_edit_title else R.string.event_new_title),
        leading = { LabelIcon(Icons.Outlined.Event) },
    )
    if (!state.ready) return
    if (!state.hasCalendarAccess) {
        PermissionPanel(calendarPermission, Icons.Outlined.EventAvailable, calendarPermissionCopy())
        return
    }
    val form = state.form
    if (form == null || state.calendars.isEmpty() && !state.editing) {
        Text(
            stringResource(if (form == null) R.string.event_missing else R.string.event_no_calendar),
            style = Pulso.type.bodyS,
            color = c.text2,
        )
        SecondaryButton(stringResource(R.string.action_close), onDone, Modifier.fillMaxWidth())
        return
    }

    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showCalendars by rememberSaveable { mutableStateOf(false) }

    // Compromisso novo: o título já abre com o foco (e o teclado), como na tarefa.
    val titleFocus = remember { FocusRequester() }
    if (!state.editing) LaunchedEffect(Unit) { titleFocus.requestFocus() }
    PulsoTextField(
        value = form.title,
        onValueChange = { v -> onChange { it.copy(title = v) } },
        placeholder = stringResource(R.string.event_title_placeholder),
        modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
    )

    // ---------- Duração (número grande + atalhos) e agenda de destino ----------
    PulsoCard(Modifier.fillMaxWidth(), level = SurfaceLevel.Panel, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.event_duration), style = Pulso.type.meta, color = c.text2, modifier = Modifier.weight(1f))
            state.calendar?.let { cal ->
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .widthIn(max = 200.dp)
                        .clip(PulsoShapes.chip)
                        .then(if (state.editing) Modifier else Modifier.clickable(role = Role.Button) { showCalendars = true })
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CalendarDot(cal.color)
                    Text(cal.name, style = Pulso.type.meta, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (!state.editing) Icon(Icons.Rounded.KeyboardArrowDown, null, tint = c.text2, modifier = Modifier.size(16.dp))
                }
            }
        }
        var durationText by remember(form.durationMinutes) { mutableStateOf(if (form.durationMinutes > 0) form.durationMinutes.toString() else "") }
        BigNumberField(
            value = durationText,
            onValueChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(4)
                durationText = digits
                onChange { it.copy(durationMinutes = digits.toIntOrNull() ?: 0) }
            },
            unit = stringResource(R.string.event_unit_min),
        )
        QuickChips(
            options = EventForm.DURATION_OPTIONS,
            selected = form.durationMinutes,
            onSelect = { m -> onChange { it.copy(durationMinutes = m) } },
            label = { PtBr.duration(it) },
        )
    }

    // ---------- Data | hora ----------
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldPanel(Icons.Outlined.CalendarMonth, PtBr.shortDate(form.date), { showDate = true }, Modifier.weight(1.25f))
        FieldPanel(Icons.Outlined.Schedule, PtBr.time(form.time), { showTime = true }, Modifier.weight(1f))
    }

    PulsoTextField(
        value = form.location,
        onValueChange = { v -> onChange { it.copy(location = v) } },
        placeholder = stringResource(R.string.event_location_placeholder),
        modifier = Modifier.fillMaxWidth(),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(stringResource(R.string.field_reminder))
        val none = stringResource(R.string.reminder_none)
        // Um lembrete fora das opções (evento criado em outro app) entra como opção extra.
        val options = (EventForm.REMINDER_OPTIONS + form.reminderMinutes).distinct().sortedBy { it ?: -1 }
        QuickChips(
            options = options,
            selected = form.reminderMinutes,
            onSelect = { m -> onChange { it.copy(reminderMinutes = m) } },
            label = { m -> if (m == null) none else if (m == 0) "Na hora" else PtBr.duration(m) },
        )
    }

    PrimaryButton(
        text = stringResource(if (state.editing) R.string.event_cta_save else R.string.event_cta_create) +
            " · " + TaskUiMapper.ctaWhen(form.date, form.time, state.today),
        onClick = onSave,
        enabled = form.isValid && !state.saving,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    )

    if (showDate) {
        PulsoDatePickerDialog(
            initial = form.date,
            onConfirm = { d -> onChange { it.copy(date = d) }; showDate = false },
            onDismiss = { showDate = false },
        )
    }
    if (showTime) {
        PulsoTimePickerDialog(
            title = stringResource(R.string.event_time_title),
            initial = form.time,
            confirmLabel = ::useTimeLabel,
            onConfirm = { t -> onChange { it.copy(time = t) }; showTime = false },
            onDismiss = { showTime = false },
        )
    }
    if (showCalendars) {
        CalendarChoiceSheet(
            title = stringResource(R.string.settings_default_calendar),
            calendars = state.calendars.map { CalendarChoice(it.id, it.name, it.account, it.color) },
            selectedId = form.calendarId,
            onSelect = { id -> onChange { it.copy(calendarId = id) }; showCalendars = false },
            onDismiss = { showCalendars = false },
        )
    }
}

/** Confirmação: halo, legenda aurora, o resultado em números, meta, cartão do lembrete e "Concluir". */
@Composable
private fun ColumnScope.EventConfirmationContent(confirmation: EventConfirmation, onDone: () -> Unit) {
    val c = Pulso.colors
    ConfirmationContent(
        caption = stringResource(if (confirmation.created) R.string.confirm_created else R.string.confirm_updated),
        value = confirmation.value,
        meta = confirmation.meta,
        modifier = Modifier.padding(top = 12.dp),
    ) {
        confirmation.reminder?.let { reminder ->
            PulsoCard(Modifier.fillMaxWidth(), level = SurfaceLevel.Panel) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Lembrete = papel de tempo: tile volt.
                    Box(Modifier.size(40.dp).background(c.voltSoft, PulsoShapes.control), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.NotificationsNone, null, tint = c.voltText, modifier = Modifier.size(20.dp))
                    }
                    Text(reminder, style = Pulso.type.body, color = c.text)
                }
            }
        }
    }
    SecondaryButton(stringResource(R.string.action_done), onDone, Modifier.fillMaxWidth().padding(top = 8.dp))
}
