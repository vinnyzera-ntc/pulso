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
}
