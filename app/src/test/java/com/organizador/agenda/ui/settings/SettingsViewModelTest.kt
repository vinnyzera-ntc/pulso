package com.organizador.agenda.ui.settings

import android.Manifest
import app.cash.turbine.test
import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.model.DeviceCalendar
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.model.EventDraft
import com.organizador.agenda.domain.model.FocusSettings
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun cal(id: Long, account: String, google: Boolean, primary: Boolean = false, write: Boolean = true, name: String = "C$id") =
        DeviceCalendar(id, name, account, if (google) DeviceCalendar.GOOGLE_ACCOUNT_TYPE else "local", id.toInt(), primary, true, write)

    @Test
    fun buildState_agrupaContasGooglePrimeiroEPrincipalNoTopo() {
        val cals = listOf(
            cal(1, "Samsung", google = false),
            cal(2, "eu@gmail.com", google = true, name = "Aniversários", write = false),
            cal(3, "eu@gmail.com", google = true, primary = true, name = "eu@gmail.com"),
        )
        val s = SettingsViewModel.buildState(AppSettings(), cals, hasAccess = true)
        assertEquals(listOf("eu@gmail.com", SettingsViewModel.OTHER_ACCOUNTS), s.accounts.map { it.account })
        assertEquals(listOf(3L, 2L), s.accounts[0].calendars.map { it.id })
        assertEquals(3L, s.defaultCalendar?.id)
        assertEquals(listOf(3L, 1L), s.writableCalendars.map { it.id })
        assertFalse(s.calendarPermissionRequested)
    }

    @Test
    fun buildState_agendaSamsungComMesmoEmailNaoEntraNoGrupoGoogle() {
        val cals = listOf(
            cal(1, "eu@gmail.com", google = true, primary = true),
            cal(2, "eu@gmail.com", google = false, name = "Samsung Calendar"),
            cal(3, "My calendar", google = false),
        )
        val s = SettingsViewModel.buildState(AppSettings(), cals, hasAccess = true)
        assertEquals(listOf(listOf(1L), listOf(3L, 2L)), s.accounts.map { g -> g.calendars.map { it.id } })
    }

    @Test
    fun buildState_permissaoJaPedida() {
        val s = SettingsViewModel.buildState(
            AppSettings(requestedPermissions = setOf(Manifest.permission.READ_CALENDAR)), emptyList(), hasAccess = false,
        )
        assertTrue(s.calendarPermissionRequested)
        assertFalse(s.hasCalendarAccess)
    }

    @Test
    fun desligarAgenda_gravaConjuntoEfetivo() = runTest(dispatcher) {
        val settings = FakeSettings()
        val calendars = FakeCalendars(listOf(cal(1, "a", true), cal(2, "a", true)))
        val vm = SettingsViewModel(settings, calendars)
        vm.state.test {
            assertEquals(setOf(1L, 2L), awaitItem().let { s -> s.accounts.flatMap { it.calendars }.filter { it.visible }.map { it.id }.toSet() })
            vm.setCalendarVisible(1, false)
            val after = awaitItem()
            assertEquals(listOf(2L), after.accounts.flatMap { it.calendars }.filter { it.visible }.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(mapOf(1L to false), settings.state.value.calendarVisibility)
    }

    @Test
    fun ajustesDeFocoELembrete_chegamAoEstado() = runTest(dispatcher) {
        val vm = SettingsViewModel(FakeSettings(), FakeCalendars(emptyList()))
        vm.state.test {
            awaitItem()
            vm.setFocus(FocusSettings(focusMinutes = 50))
            assertEquals(50, awaitItem().focus.focusMinutes)
            vm.setTaskReminderTime(LocalTime.of(8, 30))
            assertEquals(LocalTime.of(8, 30), awaitItem().taskReminderTime)
            vm.setWeekStart(DayOfWeek.MONDAY)
            assertEquals(DayOfWeek.MONDAY, awaitItem().weekStart)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class FakeSettings : SettingsRepository {
        val state = MutableStateFlow(AppSettings())
        override val settings: Flow<AppSettings> = state
        override suspend fun setCalendarVisible(id: Long, visible: Boolean) =
            state.update { it.copy(calendarVisibility = it.calendarVisibility + (id to visible)) }
        override suspend fun setDefaultCalendar(id: Long) = state.update { it.copy(defaultCalendarId = id) }
        override suspend fun setFocus(focus: FocusSettings) = state.update { it.copy(focus = focus) }
        override suspend fun setTaskReminderTime(time: LocalTime) = state.update { it.copy(taskReminderTime = time) }
        override suspend fun setEventReminderMinutes(minutes: Int) = state.update { it.copy(eventReminderMinutes = minutes) }
        override suspend fun setWeekStart(day: DayOfWeek) = state.update { it.copy(weekStart = day) }
        override suspend fun markPermissionRequested(permission: String) =
            state.update { it.copy(requestedPermissions = it.requestedPermissions + permission) }
    }

    private class FakeCalendars(initial: List<DeviceCalendar>) : CalendarRepository {
        private val calendars = MutableStateFlow(initial)
        override fun hasPermission() = true
        override fun refresh() = Unit
        override fun observeCalendars(): Flow<List<DeviceCalendar>> = calendars
        override fun observeEvents(from: Instant, to: Instant, calendarIds: Set<Long>): Flow<List<CalendarEvent>> = MutableStateFlow(emptyList())
        override suspend fun getEvent(eventId: Long): EventDetails? = null
        override suspend fun createEvent(draft: EventDraft): Long = 0
        override suspend fun updateEvent(eventId: Long, draft: EventDraft) = Unit
        override suspend fun deleteEvent(eventId: Long) = Unit
        override suspend fun cancelOccurrence(eventId: Long, instanceStart: Instant) = Unit
        override suspend fun updateOccurrence(eventId: Long, instanceStart: Instant, draft: EventDraft): Long = 0
        override suspend fun updateSeries(eventId: Long, instanceStart: Instant, draft: EventDraft) = Unit
    }
}
