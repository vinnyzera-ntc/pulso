package com.organizador.agenda.domain.task

import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.testing.TEST_ZONE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskListsTest {
    private val today = LocalDate.of(2026, 11, 10)
    private val now = LocalDateTime.of(today, LocalTime.of(15, 0))
    private fun at(h: Int, m: Int = 0) = LocalTime.of(h, m)
    private fun instant(d: LocalDate, h: Int) = d.atTime(h, 0).atZone(TEST_ZONE).toInstant()

    private fun task(
        id: Long, due: LocalDate? = null, time: LocalTime? = null, p: Priority = Priority.P4,
        doneAt: java.time.Instant? = null, tag: Long? = null,
    ) = Task(id = id, title = "T$id", priority = p, dueDate = due, dueTime = time, completedAt = doneAt, tags = listOfNotNull(tag?.let { Tag(it, "e$it") }))

    @Test
    fun isOverdue_dataPassadaOuHorarioDeHojePassado() {
        assertTrue(TaskLists.isOverdue(task(1, today.minusDays(1)), now))
        assertTrue(TaskLists.isOverdue(task(2, today, at(14, 59)), now))
        assertFalse(TaskLists.isOverdue(task(3, today, at(15, 0)), now))
        assertFalse(TaskLists.isOverdue(task(4, today), now))
        assertFalse(TaskLists.isOverdue(task(5, today.plusDays(1)), now))
        assertFalse(TaskLists.isOverdue(task(6), now))
        assertFalse("concluída nunca é atrasada", TaskLists.isOverdue(task(7, today.minusDays(3), doneAt = instant(today, 9)), now))
    }

    @Test
    fun today_atrasadasPrimeiroDepoisHoraDepoisPrioridadeConcluidasNoFim() {
        val tasks = listOf(
            task(1, today, p = Priority.P3),
            task(2, today, at(18)),
            task(3, today.minusDays(1)),
            task(4, today, at(16), Priority.P4),
            task(5, today, p = Priority.P1),
            task(6, today.minusDays(4), at(9)),
            task(7, today, doneAt = instant(today, 11)),
            task(8, today.plusDays(1)),
            task(9),
            task(10, today.plusDays(5), doneAt = instant(today, 8)),
            task(11, today.minusDays(1), doneAt = instant(today.minusDays(1), 20)),
        )
        val ids = TaskLists.today(tasks, now, TEST_ZONE).map { it.id }
        // 6 (há 4 dias), 3 (ontem), 4 (16:00), 2 (18:00), 5 (P1 sem hora), 1 (P3 sem hora), concluídas hoje: 10 (08h), 7 (11h)
        assertEquals(listOf(6L, 3L, 4L, 2L, 5L, 1L, 10L, 7L), ids)
    }

    @Test
    fun progress_contaConcluidasSobreTotalDoDia() {
        val list = TaskLists.today(
            listOf(task(1, today), task(2, today, doneAt = instant(today, 9)), task(3, today, doneAt = instant(today, 10))), now, TEST_ZONE,
        )
        val p = TaskLists.progress(list)
        assertEquals(DayProgress(2, 3), p)
        assertEquals(66, p.percent)
        assertEquals(0, DayProgress(0, 0).percent)
    }

    @Test
    fun group_todas_secoesNaOrdemEConcluidasSeparadas() {
        val tasks = listOf(
            task(1), task(2, today.plusDays(20)), task(3, today.plusDays(7)), task(4, today.plusDays(1)),
            task(5, today, at(9)), task(6, today.minusDays(2)), task(7, today.plusDays(8)),
            task(8, today, doneAt = instant(today, 9)), task(9, doneAt = instant(today, 12)),
        )
        val g = TaskLists.group(tasks, TaskFilter.All, now)
        assertEquals(
            listOf(
                TaskSection.Overdue to listOf(6L), TaskSection.Today to listOf(5L), TaskSection.Tomorrow to listOf(4L),
                TaskSection.Next7Days to listOf(3L), TaskSection.Later to listOf(7L, 2L), TaskSection.NoDate to listOf(1L),
            ),
            g.sections.map { (s, l) -> s to l.map { it.id } },
        )
        assertEquals("mais recente primeiro", listOf(9L, 8L), g.completed.map { it.id })
    }

    @Test
    fun group_filtros() {
        val tasks = listOf(
            task(1, today.minusDays(1)), task(2, today, at(9)), task(3, today, at(20)), task(4, today.plusDays(3)),
            task(5, today.plusDays(9)), task(6, tag = 7), task(7, today, doneAt = instant(today, 9), tag = 7),
        )
        fun open(f: TaskFilter) = TaskLists.group(tasks, f, now).sections.flatMap { it.second }.map { it.id }
        fun done(f: TaskFilter) = TaskLists.group(tasks, f, now).completed.map { it.id }

        assertEquals(listOf(2L, 3L), open(TaskFilter.Today))
        assertEquals(listOf(7L), done(TaskFilter.Today))
        assertEquals(listOf(2L, 3L, 4L), open(TaskFilter.Next7Days))
        // Atrasadas: ontem + a de hoje cujo horário (09:00) já passou; agrupadas por data.
        assertEquals(listOf(1L, 2L), open(TaskFilter.Overdue))
        assertEquals(emptyList<Long>(), done(TaskFilter.Overdue))
        assertEquals(listOf(6L), open(TaskFilter.NoDate))
        assertEquals(listOf(6L), open(TaskFilter.ByTag(7)))
        assertEquals(listOf(7L), done(TaskFilter.ByTag(7)))
        assertEquals(2, TaskLists.overdueCount(tasks, now))
    }
}
