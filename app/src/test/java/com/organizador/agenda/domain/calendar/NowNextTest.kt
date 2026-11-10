package com.organizador.agenda.domain.calendar

import com.organizador.agenda.testing.calendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NowNextTest {
    private fun t(hhmm: String, sec: Int = 0) = Instant.parse("2026-11-10T$hhmm:%02dZ".format(sec))

    @Test
    fun emAndamento_progressoEMinutosRestantesArredondadosParaCima() {
        val e = calendarEvent(1, t("10:00"), t("11:30"))
        val r = NowNext.pick(listOf(e), t("10:47", 10)) as NowNext.Current
        assertEquals(1L, r.event.eventId)
        assertEquals(43L, r.minutesLeft)
        assertEquals(0.524f, r.progress, 0.01f)
    }

    @Test
    fun limites_inicioContaFimNaoConta() {
        val e = calendarEvent(1, t("10:00"), t("11:00"))
        assertTrue(NowNext.pick(listOf(e), t("10:00")) is NowNext.Current)
        assertEquals(NowNext.None, NowNext.pick(listOf(e), t("11:00")))
    }

    @Test
    fun sobreposicao_escolheOQueComecouPorUltimo() {
        val long = calendarEvent(1, t("09:00"), t("12:00"))
        val short = calendarEvent(2, t("10:30"), t("11:00"))
        assertEquals(2L, (NowNext.pick(listOf(long, short), t("10:40")) as NowNext.Current).event.eventId)
    }

    @Test
    fun nadaAgora_mostraOProximo() {
        val a = calendarEvent(1, t("14:00"), t("15:00"))
        val b = calendarEvent(2, t("12:00"), t("13:00"))
        val past = calendarEvent(3, t("08:00"), t("09:00"))
        val r = NowNext.pick(listOf(a, b, past), t("11:35")) as NowNext.Next
        assertEquals(2L, r.event.eventId)
        assertEquals(25L, r.minutesUntil)
    }

    @Test
    fun diaInteiroNaoContaEDiaLivreENone() {
        val allDay = calendarEvent(1, t("00:00"), Instant.parse("2026-11-11T00:00:00Z"), allDay = true)
        assertEquals(NowNext.None, NowNext.pick(listOf(allDay), t("10:00")))
        assertEquals(NowNext.None, NowNext.pick(emptyList(), t("10:00")))
    }
}
