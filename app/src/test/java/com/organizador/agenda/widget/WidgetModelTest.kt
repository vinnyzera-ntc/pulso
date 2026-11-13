package com.organizador.agenda.widget

import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.testing.TEST_ZONE
import com.organizador.agenda.testing.calendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WidgetModelTest {
    private val today = LocalDate.of(2026, 11, 12)
    private val now = LocalDateTime.of(today, LocalTime.of(10, 0))
    private fun at(h: Int, m: Int = 0): Instant = today.atTime(h, m).atZone(TEST_ZONE).toInstant()

    @Test
    fun tarefasAbertasDeHoje_atrasadasPrimeiro_concluidasSoNoProgresso() {
        val tasks = listOf(
            Task(id = 1, title = "Às 14", dueDate = today, dueTime = LocalTime.of(14, 0)),
            Task(id = 2, title = "Atrasada", dueDate = today.minusDays(1)),
            Task(id = 3, title = "Feita", dueDate = today, completedAt = at(9)),
            Task(id = 4, title = "Amanhã", dueDate = today.plusDays(1)),
        )
        val s = WidgetModel.build(tasks, emptyList(), now, TEST_ZONE)
        assertEquals(listOf(2L, 1L), s.tasks.map { it.id })
        assertTrue(s.tasks.first().overdue)
        assertEquals("14:00", s.tasks[1].meta)
        assertEquals(1 to 3, s.done to s.total)
        assertEquals("Quinta · 12 nov", s.eyebrow)
    }

    @Test
    fun compromisso_agoraOuProximo_comHorarioAbsoluto_eProximaVirada() {
        val live = calendarEvent(1, at(9, 30), at(10, 30), "Alinhamento")
        val next = calendarEvent(2, at(15), at(16), "Dentista")
        val s = WidgetModel.build(emptyList(), listOf(live, next), now, TEST_ZONE)
        assertEquals(WidgetEvent(live = true, label = "Agora · até 10:30", title = "Alinhamento"), s.event)
        assertEquals("redesenha quando o atual termina", at(10, 30), s.nextRefresh)

        val later = WidgetModel.build(emptyList(), listOf(live, next), now.withHour(11), TEST_ZONE)
        assertEquals(WidgetEvent(live = false, label = "Próximo · 15:00", title = "Dentista"), later.event)
    }

    @Test
    fun diaLivre_semCompromisso_viraNaMeiaNoite() {
        val s = WidgetModel.build(emptyList(), emptyList(), now, TEST_ZONE)
        assertNull(s.event)
        assertTrue(s.tasks.isEmpty())
        assertEquals(today.plusDays(1).atStartOfDay(TEST_ZONE).toInstant(), s.nextRefresh)
    }
}
