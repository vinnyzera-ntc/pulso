package com.organizador.agenda.ui.settings

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.domain.calendar.CalendarSelection
import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.DeviceCalendar
import com.organizador.agenda.domain.model.FocusSettings
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

data class CalendarRowUi(
    val id: Long,
    val name: String,
    val account: String,
    val color: Int,
    val visible: Boolean,
    val canWrite: Boolean,
)

/** [account] é o e-mail da conta Google, ou [SettingsViewModel.OTHER_ACCOUNTS] para as agendas do aparelho. */
data class AccountGroupUi(val account: String, val calendars: List<CalendarRowUi>)

data class SettingsUiState(
    val loaded: Boolean = false,
    val hasCalendarAccess: Boolean = false,
    val calendarPermissionRequested: Boolean = false,
    val accounts: List<AccountGroupUi> = emptyList(),
    val defaultCalendar: CalendarRowUi? = null,
    val writableCalendars: List<CalendarRowUi> = emptyList(),
    val focus: FocusSettings = FocusSettings(),
    val taskReminderTime: LocalTime = LocalTime.of(9, 0),
    val eventReminderMinutes: Int = 10,
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val calendarRepo: CalendarRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(settingsRepo.settings, calendarRepo.observeCalendars()) { settings, calendars ->
            buildState(settings, calendars, calendarRepo.hasPermission())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onCalendarPermissionRequested() = viewModelScope.launch {
        settingsRepo.markPermissionRequested(Manifest.permission.READ_CALENDAR)
    }

    fun onCalendarPermissionResult() = calendarRepo.refresh()

    /** Recalcula ao voltar das configurações do sistema (a permissão pode ter mudado lá). */
    fun onResume() = calendarRepo.refresh()

    fun setCalendarVisible(id: Long, visible: Boolean) = viewModelScope.launch {
        val calendars = calendarRepo.observeCalendars().first()
        val configured = settingsRepo.settings.first().visibleCalendarIds
        settingsRepo.setVisibleCalendars(CalendarSelection.toggle(calendars, configured, id, visible))
    }

    fun setDefaultCalendar(id: Long) = viewModelScope.launch { settingsRepo.setDefaultCalendar(id) }
    fun setFocus(focus: FocusSettings) = viewModelScope.launch { settingsRepo.setFocus(focus) }
    fun setTaskReminderTime(time: LocalTime) = viewModelScope.launch { settingsRepo.setTaskReminderTime(time) }
    fun setEventReminderMinutes(minutes: Int) = viewModelScope.launch { settingsRepo.setEventReminderMinutes(minutes) }
    fun setWeekStart(day: DayOfWeek) = viewModelScope.launch { settingsRepo.setWeekStart(day) }

    companion object {
        /** Chave do grupo de agendas que não são do Google; a tela troca pelo rótulo traduzido. */
        const val OTHER_ACCOUNTS = ""

        val FOCUS_OPTIONS = listOf(15, 25, 45, 50, 60)
        val SHORT_BREAK_OPTIONS = listOf(3, 5, 10)
        val LONG_BREAK_OPTIONS = listOf(10, 15, 20, 30)
        val SESSIONS_OPTIONS = listOf(2, 3, 4, 5, 6)
        val EVENT_REMINDER_OPTIONS = listOf(5, 10, 15, 30, 60)
        val WEEK_START_OPTIONS = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)

        /** Montagem do estado de tela, pura (testada em JVM). */
        fun buildState(settings: AppSettings, calendars: List<DeviceCalendar>, hasAccess: Boolean): SettingsUiState {
            val visible = CalendarSelection.visibleIds(calendars, settings.visibleCalendarIds)
            val rows = calendars.map {
                CalendarRowUi(it.id, it.displayName, it.accountName, it.color, it.id in visible, it.canWrite)
            }
            val byId = rows.associateBy { it.id }
            // Um grupo por conta Google (e-mail); agendas do aparelho (Samsung, locais) num grupo só, no fim.
            val accounts = calendars
                .groupBy { if (it.isGoogle) it.accountName else OTHER_ACCOUNTS }
                .entries
                .sortedWith(compareBy({ it.key == OTHER_ACCOUNTS }, { it.key.lowercase() }))
                .map { (account, list) ->
                    AccountGroupUi(
                        account = account,
                        calendars = list.sortedWith(compareBy({ !(it.isGoogle && it.isPrimary) }, { it.displayName.lowercase() }))
                            .map { byId.getValue(it.id) },
                    )
                }
            val default = CalendarSelection.defaultForCreate(calendars, settings.defaultCalendarId)
            return SettingsUiState(
                loaded = true,
                hasCalendarAccess = hasAccess,
                calendarPermissionRequested = Manifest.permission.READ_CALENDAR in settings.requestedPermissions,
                accounts = accounts,
                defaultCalendar = default?.let { byId[it.id] },
                writableCalendars = accounts.flatMap { g -> g.calendars.filter { it.canWrite } },
                focus = settings.focus,
                taskReminderTime = settings.taskReminderTime,
                eventReminderMinutes = settings.eventReminderMinutes,
                weekStart = settings.weekStart,
            )
        }
    }
}
