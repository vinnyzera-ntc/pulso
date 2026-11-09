package com.organizador.agenda.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.organizador.agenda.data.calendar.CalendarProviderRepository
import com.organizador.agenda.domain.model.Attendee
import com.organizador.agenda.domain.model.EventDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Roda contra o Calendar Provider real do aparelho, numa agenda LOCAL "Pulso Teste"
 * (ACCOUNT_TYPE_LOCAL: não sincroniza com o Google). A agenda é criada antes e apagada depois.
 */
@RunWith(AndroidJUnit4::class)
class CalendarProviderRepositoryTest {

    @get:Rule
    val permissions: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repo = CalendarProviderRepository(context)
    private val zone = ZoneId.systemDefault()
    private var calendarId = -1L

    private fun asSyncAdapter(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    @Before
    fun createLocalCalendar() {
        deleteTestCalendars()
        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, ACCOUNT)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, ACCOUNT)
            put(Calendars.CALENDAR_DISPLAY_NAME, ACCOUNT)
            put(Calendars.CALENDAR_COLOR, Color.rgb(47, 149, 245))
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, ACCOUNT)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
            put(Calendars.CALENDAR_TIME_ZONE, zone.id)
        }
        val uri = context.contentResolver.insert(asSyncAdapter(Calendars.CONTENT_URI), values)
        calendarId = ContentUris.parseId(uri!!)
    }

    @After
    fun deleteTestCalendars() {
        context.contentResolver.delete(
            asSyncAdapter(Calendars.CONTENT_URI),
            "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(ACCOUNT, CalendarContract.ACCOUNT_TYPE_LOCAL),
        )
    }

    private val tomorrow = LocalDate.now(zone).plusDays(1)
    private fun at(h: Int, m: Int = 0) = tomorrow.atTime(LocalTime.of(h, m)).atZone(zone).toInstant()
    private val dayStart get() = tomorrow.atStartOfDay(zone).toInstant()
    private val dayEnd get() = tomorrow.plusDays(1).atStartOfDay(zone).toInstant()

    @Test
    fun agendaLocalApareceComoGravavel() = runTest {
        val cal = repo.observeCalendars().first().single { it.id == calendarId }
        assertEquals(ACCOUNT, cal.displayName)
        assertTrue(cal.canWrite)
        assertFalse(cal.isGoogle)
    }

    @Test
    fun criarLerEditarExcluir() = runTest {
        val draft = EventDraft(
            calendarId = calendarId,
            title = "Reunião com Ana",
            start = at(14),
            end = at(15),
            timeZone = zone,
            location = "Sala 3",
            description = "Pauta: orçamento",
            attendees = listOf(Attendee("Ana", "ana@example.com")),
            reminderMinutes = listOf(10, 30),
        )
        val id = repo.createEvent(draft)

        val listed = repo.observeEvents(dayStart, dayEnd, setOf(calendarId)).first()
        val e = listed.single { it.eventId == id }
        assertEquals("Reunião com Ana", e.title)
        assertEquals(at(14), e.start)
        assertEquals(at(15), e.end)
        assertEquals("Sala 3", e.location)
        assertFalse(e.allDay)
        assertFalse(e.isRecurring)

        val details = repo.getEvent(id)!!
        assertEquals("Pauta: orçamento", details.description)
        assertEquals(listOf(10, 30), details.reminderMinutes)
        assertEquals(listOf("ana@example.com"), details.attendees.map { it.email })

        // Remarcar para 16:00–17:30, trocar lembrete e participantes.
        repo.updateEvent(
            id,
            draft.copy(
                start = at(16), end = at(17, 30), reminderMinutes = listOf(15),
                attendees = listOf(Attendee("Bruno", "bruno@example.com")),
            ),
        )
        val updated = repo.getEvent(id)!!
        assertEquals(at(16), updated.event.start)
        assertEquals(at(17, 30), updated.event.end)
        assertEquals(listOf(15), updated.reminderMinutes)
        assertEquals(listOf("bruno@example.com"), updated.attendees.map { it.email })

        repo.deleteEvent(id)
        assertTrue(repo.observeEvents(dayStart, dayEnd, setOf(calendarId)).first().none { it.eventId == id })
    }

    @Test
    fun diaInteiro_gravaEmUtcEVoltaNoMesmoDia() = runTest {
        val id = repo.createEvent(
            EventDraft(calendarId = calendarId, title = "Feriado da firma", start = at(0), end = at(0), allDay = true, timeZone = zone),
        )
        val e = repo.observeEvents(dayStart.minusSeconds(86_400), dayEnd.plusSeconds(86_400), setOf(calendarId)).first()
            .single { it.eventId == id }
        assertTrue(e.allDay)
        assertEquals(tomorrow, e.startDate(zone))
        assertEquals(tomorrow.atStartOfDay(ZoneOffset.UTC).toInstant(), e.start)
        assertEquals(tomorrow.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), e.end)
    }

    @Test
    fun filtraPorAgenda_eEventoInexistente() = runTest {
        repo.createEvent(EventDraft(calendarId = calendarId, title = "X", start = at(9), end = at(10), timeZone = zone))
        assertTrue(repo.observeEvents(dayStart, dayEnd, emptySet()).first().isEmpty())
        assertTrue(repo.observeEvents(dayStart, dayEnd, setOf(-42L)).first().isEmpty())
        assertNull(repo.getEvent(Long.MAX_VALUE))
        assertNotNull(repo.observeEvents(dayStart, dayEnd, setOf(calendarId)).first().singleOrNull())
    }

    companion object {
        const val ACCOUNT = "Pulso Teste"
    }
}
