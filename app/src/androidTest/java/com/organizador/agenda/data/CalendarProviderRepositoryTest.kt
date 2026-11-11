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
import com.organizador.agenda.data.calendar.PendingEventChanges
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
    private val repo = CalendarProviderRepository(context, PendingEventChanges())
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

    /** Série diária de 5 ocorrências de 1 h, começando amanhã às [hour]h. */
    private fun insertDailySeries(title: String, hour: Int): Long {
        val uri = context.contentResolver.insert(
            CalendarContract.Events.CONTENT_URI,
            ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DTSTART, at(hour).toEpochMilli())
                put(CalendarContract.Events.DURATION, "PT1H")
                put(CalendarContract.Events.RRULE, "FREQ=DAILY;COUNT=5")
                put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            },
        )
        return ContentUris.parseId(uri!!)
    }

    private val seriesEnd get() = tomorrow.plusDays(6).atStartOfDay(zone).toInstant()
    private suspend fun series() = repo.observeEvents(dayStart, seriesEnd, setOf(calendarId)).first()
    private fun day(n: Long, h: Int, m: Int = 0) = tomorrow.plusDays(n).atTime(h, m).atZone(zone).toInstant()

    @Test
    fun recorrente_soEste_cancelarEAlterar() = runTest {
        val id = insertDailySeries("Daily", 9)
        val all = series()
        assertEquals(5, all.size)
        assertTrue(all.all { it.isRecurring && it.eventId == id })
        assertEquals(day(1, 10), all[1].end)

        // Cancelar só a 2ª ocorrência.
        repo.cancelOccurrence(id, all[1].start)
        assertEquals(listOf(day(0, 9), day(2, 9), day(3, 9), day(4, 9)), series().map { it.start })

        // Alterar só a 3ª: 9h -> 14h–15h30, com outro título e lembrete.
        val draft = EventDraft(
            calendarId = calendarId, title = "Daily (remarcada)", start = day(2, 14), end = day(2, 15, 30),
            timeZone = zone, location = "Sala 2", reminderMinutes = listOf(5),
        )
        val exceptionId = repo.updateOccurrence(id, day(2, 9), draft)
        val after = series()
        assertEquals(4, after.size)
        val moved = after.single { it.eventId == exceptionId }
        assertEquals("Daily (remarcada)", moved.title)
        assertEquals(day(2, 14), moved.start)
        assertEquals(day(2, 15, 30), moved.end)
        assertEquals("Sala 2", moved.location)
        assertFalse("a exceção é um evento avulso", moved.isRecurring)
        assertEquals(listOf(5), repo.getEvent(exceptionId)!!.reminderMinutes)
        // As demais continuam na série, às 9h, com o título antigo.
        assertEquals(listOf(day(0, 9), day(3, 9), day(4, 9)), after.filter { it.eventId == id }.map { it.start })
        assertTrue(after.filter { it.eventId == id }.all { it.title == "Daily" })
    }

    @Test
    fun recorrente_todaASerie_andaJuntoEMantemDuracao() = runTest {
        val id = insertDailySeries("Standup", 9)
        // Edita a partir da 3ª ocorrência: 9h–10h -> 9h30–10h15.
        repo.updateSeries(
            id, day(2, 9),
            EventDraft(calendarId = calendarId, title = "Standup novo", start = day(2, 9, 30), end = day(2, 10, 15), timeZone = zone),
        )
        val all = series()
        assertEquals(5, all.size)
        assertEquals((0L..4L).map { day(it, 9, 30) }, all.map { it.start })
        assertEquals((0L..4L).map { day(it, 10, 15) }, all.map { it.end })
        assertTrue(all.all { it.title == "Standup novo" && it.isRecurring })
        // getEvent de recorrente calcula o fim pela duração.
        val details = repo.getEvent(id)!!
        assertEquals(day(0, 10, 15), details.event.end)
        assertNotNull(details.rrule)
    }

    companion object {
        const val ACCOUNT = "Pulso Teste"
    }
}
