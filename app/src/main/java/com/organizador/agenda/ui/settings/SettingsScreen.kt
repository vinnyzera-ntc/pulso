package com.organizador.agenda.ui.settings

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.BuildConfig
import com.organizador.agenda.R
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.domain.time.PtBr.capitalized
import com.organizador.agenda.ui.common.ChoiceSheet
import com.organizador.agenda.ui.common.PermissionCopy
import com.organizador.agenda.ui.common.PermissionPanel
import com.organizador.agenda.ui.common.PulsoBottomSheet
import com.organizador.agenda.ui.common.PulsoTimePickerDialog
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.common.rememberPermissionRequest
import com.organizador.agenda.ui.common.useTimeLabel
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.components.LabelIcon
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.SectionLabel

private enum class SettingsSheet { DefaultCalendar, Focus, ShortBreak, LongBreak, Sessions, EventReminder, WeekStart, TaskTime }

private val CALENDAR_PERMISSIONS = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    val calendarRequest = rememberPermissionRequest(
        permissions = CALENDAR_PERMISSIONS,
        alreadyRequested = state.calendarPermissionRequested,
        onRequested = { viewModel.onCalendarPermissionRequested() },
        onResult = { viewModel.onCalendarPermissionResult() },
    )
    var sheet by rememberSaveable { mutableStateOf<SettingsSheet?>(null) }
    val c = Pulso.colors

    // Ajustes: sem aurora (tela utilitária).
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.padding(start = 6.dp, top = 6.dp)) {
            RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), onBack)
        }
        ScreenHeader(title = stringResource(R.string.settings_title), modifier = Modifier.padding(top = 0.dp))
        if (!state.loaded) return@Column

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = PulsoSpace.gutter, end = PulsoSpace.gutter, top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ---------- Agenda ----------
            item { SectionLabel(stringResource(R.string.settings_section_agenda), leading = { LabelIcon(Icons.Outlined.CalendarMonth) }) }
            if (!state.hasCalendarAccess) {
                item {
                    PermissionPanel(
                        request = calendarRequest,
                        icon = Icons.Outlined.EventAvailable,
                        copy = calendarPermissionCopy(),
                    )
                }
            }
            item {
                SettingsGroup {
                    if (state.hasCalendarAccess) {
                        val default = state.defaultCalendar
                        SettingsRow(
                            title = stringResource(R.string.settings_default_calendar),
                            value = default?.name ?: stringResource(R.string.settings_no_writable_calendar),
                            leading = default?.let { { CalendarSwatch(it.color) } },
                            stacked = true,
                            onClick = { sheet = SettingsSheet.DefaultCalendar },
                        )
                        SettingsDivider()
                    }
                    SettingsRow(
                        title = stringResource(R.string.settings_week_start),
                        value = PtBr.weekday(state.weekStart).capitalized(),
                        onClick = { sheet = SettingsSheet.WeekStart },
                    )
                }
            }
            if (state.hasCalendarAccess) {
                item {
                    Spacer(Modifier.height(14.dp))
                    SectionLabel(stringResource(R.string.settings_section_visible), leading = { LabelIcon(Icons.Outlined.Visibility) })
                }
                if (state.accounts.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.settings_no_calendars),
                            style = Pulso.type.bodyS,
                            color = c.text2,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                items(state.accounts, key = { it.account }) { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            group.account.ifEmpty { stringResource(R.string.settings_other_calendars) },
                            style = Pulso.type.meta,
                            color = c.text2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        )
                        SettingsGroup {
                            group.calendars.forEachIndexed { i, cal ->
                                if (i > 0) SettingsDivider()
                                SettingsToggleRow(
                                    title = cal.name,
                                    subtitle = if (cal.canWrite) null else stringResource(R.string.settings_read_only),
                                    checked = cal.visible,
                                    onCheckedChange = { viewModel.setCalendarVisible(cal.id, it) },
                                    leading = { CalendarSwatch(cal.color) },
                                )
                            }
                        }
                    }
                }
            }

            // ---------- Lembretes ----------
            item {
                Spacer(Modifier.height(14.dp))
                SectionLabel(stringResource(R.string.settings_section_reminders), leading = { LabelIcon(Icons.Outlined.NotificationsNone) })
            }
            item {
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.settings_task_reminder_time),
                        value = PtBr.time(state.taskReminderTime),
                        onClick = { sheet = SettingsSheet.TaskTime },
                    )
                    SettingsDivider()
                    SettingsRow(
                        title = stringResource(R.string.settings_event_reminder),
                        value = PtBr.reminderBefore(state.eventReminderMinutes),
                        onClick = { sheet = SettingsSheet.EventReminder },
                    )
                }
            }

            // ---------- Foco ----------
            item {
                Spacer(Modifier.height(14.dp))
                SectionLabel(stringResource(R.string.settings_section_focus), leading = { LabelIcon(Icons.Outlined.Bolt) })
            }
            item {
                val f = state.focus
                SettingsGroup {
                    SettingsRow(stringResource(R.string.settings_focus_minutes), PtBr.duration(f.focusMinutes), { sheet = SettingsSheet.Focus })
                    SettingsDivider()
                    SettingsRow(stringResource(R.string.settings_short_break), PtBr.duration(f.shortBreakMinutes), { sheet = SettingsSheet.ShortBreak })
                    SettingsDivider()
                    SettingsRow(stringResource(R.string.settings_long_break), PtBr.duration(f.longBreakMinutes), { sheet = SettingsSheet.LongBreak })
                    SettingsDivider()
                    SettingsRow(stringResource(R.string.settings_sessions_before_long), f.sessionsBeforeLongBreak.toString(), { sheet = SettingsSheet.Sessions })
                }
            }

            item {
                Text(
                    stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    style = Pulso.type.meta,
                    color = c.text3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp).navigationBarsPadding(),
                )
            }
        }
    }

    val dismiss = { sheet = null }
    val f = state.focus
    when (sheet) {
        SettingsSheet.DefaultCalendar -> DefaultCalendarSheet(
            calendars = state.writableCalendars,
            selectedId = state.defaultCalendar?.id,
            onSelect = { viewModel.setDefaultCalendar(it); dismiss() },
            onDismiss = dismiss,
        )
        SettingsSheet.Focus -> ChoiceSheet(
            stringResource(R.string.settings_focus_minutes), SettingsViewModel.FOCUS_OPTIONS, f.focusMinutes,
            { "$it min" }, { viewModel.setFocus(f.copy(focusMinutes = it)) }, dismiss,
        )
        SettingsSheet.ShortBreak -> ChoiceSheet(
            stringResource(R.string.settings_short_break), SettingsViewModel.SHORT_BREAK_OPTIONS, f.shortBreakMinutes,
            { "$it min" }, { viewModel.setFocus(f.copy(shortBreakMinutes = it)) }, dismiss,
        )
        SettingsSheet.LongBreak -> ChoiceSheet(
            stringResource(R.string.settings_long_break), SettingsViewModel.LONG_BREAK_OPTIONS, f.longBreakMinutes,
            { "$it min" }, { viewModel.setFocus(f.copy(longBreakMinutes = it)) }, dismiss,
        )
        SettingsSheet.Sessions -> ChoiceSheet(
            stringResource(R.string.settings_sessions_before_long), SettingsViewModel.SESSIONS_OPTIONS, f.sessionsBeforeLongBreak,
            { it.toString() }, { viewModel.setFocus(f.copy(sessionsBeforeLongBreak = it)) }, dismiss,
            description = stringResource(R.string.settings_sessions_description),
        )
        SettingsSheet.EventReminder -> ChoiceSheet(
            stringResource(R.string.settings_event_reminder), SettingsViewModel.EVENT_REMINDER_OPTIONS, state.eventReminderMinutes,
            { PtBr.duration(it) }, { viewModel.setEventReminderMinutes(it) }, dismiss,
            description = stringResource(R.string.settings_event_reminder_description),
        )
        SettingsSheet.WeekStart -> ChoiceSheet(
            stringResource(R.string.settings_week_start), SettingsViewModel.WEEK_START_OPTIONS, state.weekStart,
            { PtBr.weekday(it).capitalized() }, { viewModel.setWeekStart(it) }, dismiss,
        )
        SettingsSheet.TaskTime -> PulsoTimePickerDialog(
            title = stringResource(R.string.settings_task_reminder_time),
            initial = state.taskReminderTime,
            confirmLabel = ::useTimeLabel,
            onConfirm = { viewModel.setTaskReminderTime(it); dismiss() },
            onDismiss = dismiss,
        )
        null -> Unit
    }
}

@Composable
private fun calendarPermissionCopy() = PermissionCopy(
    title = stringResource(R.string.perm_calendar_title),
    message = stringResource(R.string.perm_calendar_message),
    requestLabel = stringResource(R.string.perm_calendar_request),
    deniedTitle = stringResource(R.string.perm_calendar_denied_title),
    deniedMessage = stringResource(R.string.perm_calendar_denied_message),
    blockedTitle = stringResource(R.string.perm_calendar_blocked_title),
    blockedMessage = stringResource(R.string.perm_calendar_blocked_message),
    openSettingsLabel = stringResource(R.string.perm_open_settings),
)

/** Lista das agendas com escrita; a escolhida tem o anel ember (seleção de valor). */
@Composable
private fun DefaultCalendarSheet(
    calendars: List<CalendarRowUi>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Pulso.colors
    PulsoBottomSheet(onDismiss) {
        SectionLabel(stringResource(R.string.settings_default_calendar))
        Column(
            Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()).selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            calendars.forEach { cal ->
                val on = cal.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(if (on) c.emberSoft else c.surface2, PulsoShapes.control)
                        .border(if (on) 1.5.dp else 1.dp, if (on) c.ember else c.line, PulsoShapes.control)
                        .selectable(selected = on, role = Role.RadioButton) { onSelect(cal.id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CalendarSwatch(cal.color)
                    Column(Modifier.weight(1f)) {
                        Text(cal.name, style = Pulso.type.body, color = if (on) c.emberText else c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(cal.account, style = Pulso.type.meta, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (on) Icon(Icons.Outlined.Check, null, tint = c.emberText, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
