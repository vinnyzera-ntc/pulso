package com.organizador.agenda.widget

import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.testing.TEST_ZONE
import com.organizador.agenda.testing.calendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    // ---------- Próximos dias ----------

    @Test
    fun proximosDias_abertosDentroDoLimite_comDataEmText2() {
        val tasks = listOf(
            Task(id = 1, title = "Hoje", dueDate = today),
            Task(id = 2, title = "Sexta", dueDate = today.plusDays(1), dueTime = LocalTime.of(10, 0)),
            Task(id = 3, title = "Domingo", dueDate = today.plusDays(3)),
            Task(id = 4, title = "Longe", dueDate = today.plusDays(10)),
            Task(id = 5, title = "Feita", dueDate = today.plusDays(2), completedAt = at(9)),
        )
        val s = WidgetModel.build(tasks, emptyList(), now, TEST_ZONE, upcomingDays = 7)
        assertEquals(listOf(1L), s.tasks.map { it.id })
        assertEquals(listOf(2L, 3L), s.upcoming.map { it.id })
        assertEquals(listOf("Amanhã · 10:00", "Dom, 15 nov"), s.upcoming.map { it.meta })
        assertTrue(s.upcoming.all { it.future && !it.overdue })
        assertEquals("a de depois concluída hoje conta no progresso de hoje", 1 to 2, s.done to s.total)

        assertEquals(listOf(2L, 3L, 4L), WidgetModel.build(tasks, emptyList(), now, TEST_ZONE, upcomingDays = 14).upcoming.map { it.id })
        assertTrue(WidgetModel.build(tasks, emptyList(), now, TEST_ZONE, upcomingDays = 0).upcoming.isEmpty())
    }

    private fun wt(id: Long, future: Boolean = false) = WidgetTask(id, "T$id", null, overdue = false, future = future)
    private fun state(today: Int, upcoming: Int) = WidgetState(
        "", 0, today, null,
        tasks = (1..today).map { wt(it.toLong()) },
        upcoming = (1..upcoming).map { wt(100L + it, future = true) },
        nextRefresh = Instant.EPOCH,
    )
    private fun WidgetLayout.ids() = Triple(this.today.map { it.id }, this.upcoming.map { it.id }, more)

    @Test
    fun layout_tudoCabe_semMais() {
        val l = WidgetModel.layout(state(2, 2), heightDp = 2 * 44 + 26 + 2 * 44, rowDp = 44, labelDp = 26)
        assertEquals(Triple(listOf(1L, 2L), listOf(101L, 102L), 0), l.ids())
        assertTrue(l.upcomingLabel)
    }

    @Test
    fun layout_hojePrimeiro_proximosSoComRotuloEUmaLinha_eUmaLinhaParaOMais() {
        // 5 linhas de altura: 2 de hoje, rótulo + 1 próximo e a linha do "+N mais".
        val l = WidgetModel.layout(state(2, 5), heightDp = 5 * 44, rowDp = 44, labelDp = 26)
        assertEquals(Triple(listOf(1L, 2L), listOf(101L), 4), l.ids())

        // Sobra menos que rótulo + uma linha: os próximos dias ficam todos no "+N mais", sem rótulo.
        val tight = WidgetModel.layout(state(2, 3), heightDp = 3 * 44 + 20, rowDp = 44, labelDp = 26)
        assertEquals(Triple(listOf(1L, 2L), emptyList<Long>(), 3), tight.ids())
        assertFalse(tight.upcomingLabel)
    }

    @Test
    fun layout_hojeNaoCabeInteiro_proximosNemEntram() {
        val l = WidgetModel.layout(state(6, 3), heightDp = 4 * 44, rowDp = 44, labelDp = 26)
        assertEquals(Triple(listOf(1L, 2L, 3L), emptyList<Long>(), 6), l.ids())
    }

    @Test
    fun layout_semProximos_comportamentoAntigo() {
        assertEquals(Triple(listOf(1L, 2L, 3L), emptyList<Long>(), 0), WidgetModel.layout(state(3, 0), 3 * 44, 44, 26).ids())
        assertEquals(Triple(listOf(1L, 2L), emptyList<Long>(), 2), WidgetModel.layout(state(4, 0), 3 * 44, 44, 26).ids())
        assertEquals("sem altura: tudo no +N", Triple(emptyList<Long>(), emptyList<Long>(), 4), WidgetModel.layout(state(4, 0), 0, 44, 26).ids())
    }

    @Test
    fun layout_hojeVazio_proximosComRotulo() {
        val l = WidgetModel.layout(state(0, 4), heightDp = 3 * 44, rowDp = 44, labelDp = 26)
        assertEquals(Triple(emptyList<Long>(), listOf(101L), 3), l.ids())
        assertTrue(l.upcomingLabel)
    }
}
