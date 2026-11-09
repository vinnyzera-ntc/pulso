package com.organizador.agenda.data

import com.organizador.agenda.data.calendar.CalendarProviderRepository
import com.organizador.agenda.data.local.Converters
import com.organizador.agenda.data.local.normalizeTagName
import com.organizador.agenda.data.local.toEntity
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MappersAndParsingTest {

    @Test
    fun normalizeTagName_tiraCerquilhaEspacosEMaiusculas() {
        assertEquals("pessoal", normalizeTagName(" #Pessoal "))
        assertEquals("trabalho", normalizeTagName("trabalho"))
        assertEquals("", normalizeTagName("#"))
    }

    @Test
    fun toEntity_horaSemDataEDescartada() {
        val e = Task(title = "  Ler  ", dueTime = LocalTime.NOON, priority = Priority.P2).toEntity(Instant.EPOCH)
        assertNull(e.dueTime)
        assertEquals("Ler", e.title)
        assertEquals(2, e.priority)
    }

    @Test
    fun toEntity_novaTarefaGanhaCreatedAtAgora_existenteMantem() {
        val now = Instant.parse("2026-11-09T12:00:00Z")
        val created = Instant.parse("2026-11-01T08:00:00Z")
        assertEquals(now, Task(title = "a").toEntity(now).createdAt)
        assertEquals(created, Task(id = 5, title = "a", createdAt = created).toEntity(now).createdAt)
    }

    @Test
    fun converters_idaEVolta() {
        val c = Converters()
        val d = LocalDate.of(2026, 11, 9)
        val t = LocalTime.of(14, 30)
        val dt = LocalDateTime.of(2026, 11, 9, 14, 30)
        assertEquals(d, c.longToDate(c.dateToLong(d)))
        assertEquals(t, c.intToTime(c.timeToInt(t)))
        assertEquals(dt, c.stringToDateTime(c.dateTimeToString(dt)))
        assertNull(c.longToDate(null))
    }

    @Test
    fun parseDuration_formatosRfc2445() {
        assertEquals(3_600_000L, CalendarProviderRepository.parseDurationMillis("P3600S"))
        assertEquals(3_600_000L, CalendarProviderRepository.parseDurationMillis("PT1H"))
        assertEquals(5_400_000L, CalendarProviderRepository.parseDurationMillis("PT1H30M"))
        assertEquals(86_400_000L, CalendarProviderRepository.parseDurationMillis("P1D"))
        assertEquals(604_800_000L, CalendarProviderRepository.parseDurationMillis("P1W"))
        assertEquals(0L, CalendarProviderRepository.parseDurationMillis(null))
        assertEquals(0L, CalendarProviderRepository.parseDurationMillis("lixo"))
    }
}
