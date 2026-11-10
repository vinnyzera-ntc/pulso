package com.organizador.agenda.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PtBrTest {

    @Test
    fun eyebrowDate_usaDiaDaSemanaSemFeiraEMesCurto() {
        assertEquals("Segunda · 9 nov", PtBr.eyebrowDate(LocalDate.of(2026, 11, 9)))
        assertEquals("Sábado · 14 mar", PtBr.eyebrowDate(LocalDate.of(2026, 3, 14)))
    }

    @Test
    fun shortDate_formatoDeConfirmacao() {
        assertEquals("Ter, 10 nov", PtBr.shortDate(LocalDate.of(2026, 11, 10)))
        assertEquals("Dom, 1 fev", PtBr.shortDate(LocalDate.of(2026, 2, 1)))
    }

    @Test
    fun time_24hComZeroAEsquerda() {
        assertEquals("14:00", PtBr.time(LocalTime.of(14, 0)))
        assertEquals("09:05", PtBr.time(LocalTime.of(9, 5)))
        assertEquals("00:00", PtBr.time(LocalTime.MIDNIGHT))
    }

    @Test
    fun greeting_limitesDosPeriodos() {
        assertEquals("Boa noite", PtBr.greeting(LocalTime.of(4, 59)))
        assertEquals("Bom dia", PtBr.greeting(LocalTime.of(5, 0)))
        assertEquals("Bom dia", PtBr.greeting(LocalTime.of(11, 59)))
        assertEquals("Boa tarde", PtBr.greeting(LocalTime.of(12, 0)))
        assertEquals("Boa tarde", PtBr.greeting(LocalTime.of(17, 59)))
        assertEquals("Boa noite", PtBr.greeting(LocalTime.of(18, 0)))
    }

    @Test
    fun weekdayShort_eMonthShort() {
        assertEquals("sáb", PtBr.weekdayShort(java.time.DayOfWeek.SATURDAY))
        assertEquals("mar", PtBr.monthShort(java.time.Month.MARCH))
        assertEquals("novembro", PtBr.month(java.time.Month.NOVEMBER))
    }

    @Test
    fun duration_minutosEHoras() {
        assertEquals("5 min", PtBr.duration(5))
        assertEquals("1 h", PtBr.duration(60))
        assertEquals("1 h 30 min", PtBr.duration(90))
        assertEquals("2 h", PtBr.duration(120))
    }

    @Test
    fun reminderBefore_naHoraEAntes() {
        assertEquals("Na hora", PtBr.reminderBefore(0))
        assertEquals("10 min antes", PtBr.reminderBefore(10))
        assertEquals("1 h antes", PtBr.reminderBefore(60))
    }

    @Test
    fun relativeDay_hojeAmanhaOntemEDatas() {
        val today = LocalDate.of(2026, 11, 10)
        assertEquals("hoje", PtBr.relativeDay(today, today))
        assertEquals("amanhã", PtBr.relativeDay(today.plusDays(1), today))
        assertEquals("ontem", PtBr.relativeDay(today.minusDays(1), today))
        assertEquals("há 6 dias", PtBr.relativeDay(today.minusDays(6), today))
        assertEquals("ter, 3 nov", PtBr.relativeDay(today.minusDays(7), today))
        assertEquals("qui, 12 nov", PtBr.relativeDay(today.plusDays(2), today))
        assertEquals("sex, 1 jan 2027", PtBr.relativeDay(LocalDate.of(2027, 1, 1), today))
    }

    @Test
    fun inMinutes_eLembreteEmDias() {
        assertEquals("em 25 min", PtBr.inMinutes(25))
        assertEquals("em 1 h 20 min", PtBr.inMinutes(80))
        assertEquals("agora", PtBr.inMinutes(0))
        assertEquals("1 dia antes", PtBr.reminderBefore(1440))
        assertEquals("2 dias antes", PtBr.reminderBefore(2880))
    }
}
