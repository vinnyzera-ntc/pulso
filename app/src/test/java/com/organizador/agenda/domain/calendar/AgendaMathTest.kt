package com.organizador.agenda.domain.calendar

import com.organizador.agenda.testing.TEST_ZONE
import com.organizador.agenda.testing.calendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class AgendaMathTest {
    private val day = LocalDate.of(2026, 11, 10) // terça
    private fun at(h: Int, m: Int = 0, d: LocalDate = day) = d.atTime(h, m).atZone(TEST_ZONE).toInstant()
    private fun slot(id: Long, start: Int, end: Int) = TimedSlot(calendarEvent(id, at(0), at(0)), start, end)

    @Test
    fun weekOf_domingoESegunda() {
        assertEquals(LocalDate.of(2026, 11, 8), AgendaMath.weekOf(day, DayOfWeek.SUNDAY).first())
        assertEquals(LocalDate.of(2026, 11, 14), AgendaMath.weekOf(day, DayOfWeek.SUNDAY).last())
        assertEquals(LocalDate.of(2026, 11, 9), AgendaMath.weekOf(day, DayOfWeek.MONDAY).first())
        // O próprio dia de início fica na sua semana.
        assertEquals(LocalDate.of(2026, 11, 8), AgendaMath.weekOf(LocalDate.of(2026, 11, 8), DayOfWeek.SUNDAY).first())
    }

    @Test
    fun monthGrid_cobreOMesComSemanasCompletas() {
        val grid = AgendaMath.monthGrid(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)
        assertEquals(5, grid.size)
        assertEquals(LocalDate.of(2026, 11, 1), grid.first().first()) // 1º de nov de 2026 é domingo
        assertEquals(LocalDate.of(2026, 12, 5), grid.last().last())
        val monday = AgendaMath.monthGrid(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        assertEquals(6, monday.size)
        assertEquals(LocalDate.of(2026, 10, 26), monday.first().first())
        assertTrue(monday.all { it.size == 7 })
        // Fevereiro de 2026 começa num domingo e tem 28 dias: exatamente 4 semanas.
        assertEquals(4, AgendaMath.monthGrid(YearMonth.of(2026, 2), DayOfWeek.SUNDAY).size)
    }

    @Test
    fun forDay_recortaNosLimitesDoDiaESeparaDiaInteiro() {
        val overnight = calendarEvent(1, at(22, 0, day.minusDays(1)), at(1, 30))
        val normal = calendarEvent(2, at(14), at(15))
        val late = calendarEvent(3, at(23), at(2, 0, day.plusDays(1)))
        val other = calendarEvent(4, at(10, 0, day.plusDays(1)), at(11, 0, day.plusDays(1)))
        val allDay = calendarEvent(
            5, day.atStartOfDay(ZoneOffset.UTC).toInstant(), day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), allDay = true,
        )
        val r = AgendaMath.forDay(listOf(normal, other, late, allDay, overnight), day, TEST_ZONE)
        assertEquals(listOf(5L), r.allDay.map { it.eventId })
        assertEquals(listOf(Triple(1L, 0, 90), Triple(2L, 840, 900), Triple(3L, 1380, 1440)), r.timed.map { Triple(it.event.eventId, it.startMin, it.endMin) })
    }

    @Test
    fun occursOn_diaInteiroNaoVazaParaODiaSeguinte_eFimExclusivo() {
        val allDay = calendarEvent(
            1, day.atStartOfDay(ZoneOffset.UTC).toInstant(), day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(), allDay = true,
        )
        assertTrue(AgendaMath.occursOn(allDay, day, TEST_ZONE))
        assertFalse(AgendaMath.occursOn(allDay, day.plusDays(1), TEST_ZONE))
        assertFalse(AgendaMath.occursOn(allDay, day.minusDays(1), TEST_ZONE))
        val untilMidnight = calendarEvent(2, at(22), at(0, 0, day.plusDays(1)))
        assertFalse("termina à meia-noite: não conta no dia seguinte", AgendaMath.occursOn(untilMidnight, day.plusDays(1), TEST_ZONE))
    }

    @Test
    fun layout_semSobreposicaoUmaColuna() {
        val r = AgendaMath.layout(listOf(slot(1, 540, 600), slot(2, 600, 660)))
        assertEquals(Placement(0, 1), r[1])
        assertEquals(Placement(0, 1), r[2])
    }

    @Test
    fun layout_doisSobrepostosDividemLargura() {
        val r = AgendaMath.layout(listOf(slot(1, 540, 630), slot(2, 600, 660)))
        assertEquals(Placement(0, 2), r[1])
        assertEquals(Placement(1, 2), r[2])
    }

    @Test
    fun layout_colunaReaproveitadaEGrupoCompartilhaLargura() {
        // 1: 9–11, 2: 9:30–10, 3: 10–11 (cabe na coluna de 2), 4: 12–13 (grupo novo)
        val r = AgendaMath.layout(listOf(slot(1, 540, 660), slot(2, 570, 600), slot(3, 600, 660), slot(4, 720, 780)))
        assertEquals(Placement(0, 2), r[1])
        assertEquals(Placement(1, 2), r[2])
        assertEquals(Placement(1, 2), r[3])
        assertEquals(Placement(0, 1), r[4])
    }

    @Test
    fun layout_tresSimultaneos_eCurtoOcupaAlturaMinima() {
        val r = AgendaMath.layout(listOf(slot(1, 600, 660), slot(2, 600, 660), slot(3, 600, 660)))
        assertEquals(setOf(0, 1, 2), r.values.map { it.column }.toSet())
        assertTrue(r.values.all { it.columns == 3 })
        // Evento de 5 min às 10:00 e outro às 10:15: o bloco mínimo (30 min) faz com que se sobreponham.
        val short = AgendaMath.layout(listOf(slot(1, 600, 605), slot(2, 615, 675)))
        assertEquals(2, short.getValue(1).columns)
    }

    @Test
    fun snap_eDragTarget() {
        assertEquals(600, AgendaMath.snap(607))
        assertEquals(615, AgendaMath.snap(608))
        assertEquals(0, AgendaMath.snap(-7))
        // 14:00 arrastado +38 min -> 14:45 (encaixe de 15)
        assertEquals(885, AgendaMath.dragTarget(840, 60, 38))
        // Não passa da meia-noite nem fica antes de 00:00.
        assertEquals(1380, AgendaMath.dragTarget(1320, 60, 500))
        assertEquals(0, AgendaMath.dragTarget(30, 60, -200))
    }

    @Test
    fun weekNumber_iso() {
        assertEquals(46, AgendaMath.weekNumber(day))
    }
}
