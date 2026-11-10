package com.organizador.agenda.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.FocusSettings
import com.organizador.agenda.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    private object Keys {
        val shownCalendars = stringSetPreferencesKey("calendars_shown")
        val hiddenCalendars = stringSetPreferencesKey("calendars_hidden")
        val defaultCalendar = longPreferencesKey("default_calendar")
        val focusMinutes = intPreferencesKey("focus_minutes")
        val shortBreak = intPreferencesKey("short_break_minutes")
        val longBreak = intPreferencesKey("long_break_minutes")
        val sessionsBeforeLong = intPreferencesKey("sessions_before_long_break")
        val taskReminderSecondOfDay = intPreferencesKey("task_reminder_second_of_day")
        val eventReminderMinutes = intPreferencesKey("event_reminder_minutes")
        val weekStart = intPreferencesKey("week_start")
        val requestedPermissions = stringSetPreferencesKey("requested_permissions")
    }

    override val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        val f = d.focus
        AppSettings(
            calendarVisibility = buildMap {
                p[Keys.shownCalendars]?.forEach { id -> id.toLongOrNull()?.let { put(it, true) } }
                p[Keys.hiddenCalendars]?.forEach { id -> id.toLongOrNull()?.let { put(it, false) } }
            },
            defaultCalendarId = p[Keys.defaultCalendar],
            focus = FocusSettings(
                focusMinutes = p[Keys.focusMinutes] ?: f.focusMinutes,
                shortBreakMinutes = p[Keys.shortBreak] ?: f.shortBreakMinutes,
                longBreakMinutes = p[Keys.longBreak] ?: f.longBreakMinutes,
                sessionsBeforeLongBreak = p[Keys.sessionsBeforeLong] ?: f.sessionsBeforeLongBreak,
            ),
            taskReminderTime = p[Keys.taskReminderSecondOfDay]?.let { LocalTime.ofSecondOfDay(it.toLong()) } ?: d.taskReminderTime,
            eventReminderMinutes = p[Keys.eventReminderMinutes] ?: d.eventReminderMinutes,
            weekStart = p[Keys.weekStart]?.let(DayOfWeek::of) ?: d.weekStart,
            requestedPermissions = p[Keys.requestedPermissions] ?: emptySet(),
        )
    }

    override suspend fun setCalendarVisible(id: Long, visible: Boolean) {
        val key = id.toString()
        store.edit {
            val shown = it[Keys.shownCalendars] ?: emptySet()
            val hidden = it[Keys.hiddenCalendars] ?: emptySet()
            it[Keys.shownCalendars] = if (visible) shown + key else shown - key
            it[Keys.hiddenCalendars] = if (visible) hidden - key else hidden + key
        }
    }

    override suspend fun setDefaultCalendar(id: Long) {
        store.edit { it[Keys.defaultCalendar] = id }
    }

    override suspend fun setFocus(focus: FocusSettings) {
        store.edit {
            it[Keys.focusMinutes] = focus.focusMinutes
            it[Keys.shortBreak] = focus.shortBreakMinutes
            it[Keys.longBreak] = focus.longBreakMinutes
            it[Keys.sessionsBeforeLong] = focus.sessionsBeforeLongBreak
        }
    }

    override suspend fun setTaskReminderTime(time: LocalTime) {
        store.edit { it[Keys.taskReminderSecondOfDay] = time.withSecond(0).withNano(0).toSecondOfDay() }
    }

    override suspend fun setEventReminderMinutes(minutes: Int) {
        store.edit { it[Keys.eventReminderMinutes] = minutes }
    }

    override suspend fun setWeekStart(day: DayOfWeek) {
        store.edit { it[Keys.weekStart] = day.value }
    }

    override suspend fun markPermissionRequested(permission: String) {
        store.edit { it[Keys.requestedPermissions] = (it[Keys.requestedPermissions] ?: emptySet()) + permission }
    }
}
