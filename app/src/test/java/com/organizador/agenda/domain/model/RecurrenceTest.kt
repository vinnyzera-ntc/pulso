package com.organizador.agenda.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

class RecurrenceTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test
    fun diaria_eACadaN() {
        assertEquals(d(11, 13), Recurrence.daily().nextAfter(d(11, 12), d(11, 12)))
        assertEquals(d(11, 15), Recurrence.daily(3).nextAfter(d(11, 12), d(11, 12)))
        // Atrasada 4 dias, a cada 3: próxima depois de hoje (16) na série 12, 15, 18...
        assertEquals(d(11, 18), Recurrence.daily(3).nextAfter(d(11, 16), d(11, 12)))
    }

    @Test
    fun semanal_variosDias() {
        val r = Recurrence.weekly(MONDAY, WEDNESDAY, FRIDAY)
        // 12/11/2026 é quinta.
        assertEquals(d(11, 13), r.nextAfter(d(11, 12), d(11, 11)))
        assertEquals(d(11, 16), r.nextAfter(d(11, 13), d(11, 11)))
    }

    @Test
    fun semanal_aCadaDuasSemanas() {
        val r = Recurrence.weekly(MONDAY, interval = 2)
        assertEquals(d(11, 23), r.nextAfter(d(11, 9), d(11, 9)))
        assertEquals(d(11, 23), r.nextAfter(d(11, 16), d(11, 9)))
    }

    @Test
    fun mensal_dia31EmMesCurto_usaOUltimoDia() {
        val r = Recurrence.monthly(31)
        assertEquals(d(11, 30), r.nextAfter(d(10, 31), d(10, 31)))
        assertEquals(d(12, 31), r.nextAfter(d(11, 30), d(10, 31)))
        assertEquals(d(2, 28, 2027), r.nextAfter(d(1, 31, 2027), d(10, 31)))
    }

    @Test
    fun mensal_aCadaTresMeses() {
        assertEquals(d(2, 5, 2027), Recurrence.monthly(5, 3).nextAfter(d(11, 5), d(11, 5)))
    }

    @Test
    fun anual_29DeFevereiro() {
        val anchor = d(2, 29, 2028)
        assertEquals(d(2, 28, 2029), Recurrence.yearly().nextAfter(anchor, anchor))
    }

    @Test
    fun primeiraData_aPartirDeHoje() {
        assertEquals(d(11, 16), Recurrence.weekly(MONDAY).firstOnOrAfter(d(11, 12)))
        assertEquals(d(12, 5), Recurrence.monthly(5).firstOnOrAfter(d(11, 12)))
        assertEquals(d(11, 12), Recurrence.monthly(12).firstOnOrAfter(d(11, 12)))
    }

    @Test
    fun codificaEDecodifica() {
        listOf(
            Recurrence.daily(2), Recurrence.weekly(MONDAY, FRIDAY), Recurrence.monthly(5, 3), Recurrence.monthly(null), Recurrence.yearly(),
        ).forEach { assertEquals(it, Recurrence.decode(it.encode())) }
        assertNull(Recurrence.decode("X|1"))
        assertNull(Recurrence.decode("lixo"))
        assertNull(Recurrence.decode(null))
    }

    @Test
    fun tarefa_proximoVencimento() {
        val today = d(11, 12)
        val t = Task(title = "Aluguel", dueDate = d(11, 5), recurrence = Recurrence.monthly(5))
        assertEquals("atrasada: pula para depois de hoje", d(12, 5), t.nextDue(today))
        assertEquals("adiantada: próxima depois do vencimento", d(1, 5, 2027), t.copy(dueDate = d(12, 5)).nextDue(today))
        assertNull(Task(title = "Uma vez", dueDate = today).nextDue(today))
        assertNull(Task(title = "Sem data", recurrence = Recurrence.daily()).nextDue(today))
    }
}
