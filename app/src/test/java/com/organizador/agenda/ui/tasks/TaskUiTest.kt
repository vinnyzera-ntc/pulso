package com.organizador.agenda.ui.tasks

import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Subtask
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.task.TaskFilter
import com.organizador.agenda.domain.task.TaskSection
import com.organizador.agenda.ui.pulso.components.ChipTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskUiTest {
    private val today = LocalDate.of(2026, 11, 10) // terça
    private val now = LocalDateTime.of(today, LocalTime.of(15, 0))
    private fun chip(due: LocalDate?, time: LocalTime? = null) = TaskUiMapper.dueChip(Task(title = "t", dueDate = due, dueTime = time), now)

    @Test
    fun dueChip_papeisDeCor() {
        assertNull(chip(null))
        assertNull("hoje sem horário não ganha chip", chip(today))
        assertEquals(TaskChipUi("16:30", ChipTone.Volt, ChipIcon.Clock), chip(today, LocalTime.of(16, 30)))
        assertEquals(TaskChipUi("Atrasada · 09:00", ChipTone.Flare, ChipIcon.Flag), chip(today, LocalTime.of(9, 0)))
        assertEquals(TaskChipUi("Atrasada · ontem", ChipTone.Flare, ChipIcon.Flag), chip(today.minusDays(1)))
        assertEquals("Atrasada · há 3 dias", chip(today.minusDays(3), LocalTime.of(8, 0))!!.text)
        assertEquals("Atrasada · ter, 27 out", chip(LocalDate.of(2026, 10, 27))!!.text)
        assertEquals(TaskChipUi("Amanhã · 10:00", ChipTone.Neutral, ChipIcon.Calendar), chip(today.plusDays(1), LocalTime.of(10, 0)))
        assertEquals("Sex, 13 nov", chip(today.plusDays(3))!!.text)
        assertEquals("Sex, 15 jan 2027", chip(LocalDate.of(2027, 1, 15))!!.text)
    }

    @Test
    fun row_chipsDePrazoSubtarefasEEtiquetas() {
        val task = Task(
            id = 4, title = "Comprar presente", priority = Priority.P2, dueDate = today.plusDays(1),
            subtasks = listOf(Subtask(1, "a", done = true), Subtask(2, "b")), tags = listOf(Tag(1, "pessoal")),
        )
        val row = TaskUiMapper.row(task, now)
        assertEquals(listOf("Amanhã", "1/2", "#pessoal"), row.chips.map { it.text })
        assertEquals(Priority.P2, row.priority)
        assertFalse(row.done)
        assertTrue(TaskUiMapper.row(task.copy(completedAt = Instant.EPOCH), now).done)
    }

    @Test
    fun ctaWhen_resultadoNoBotao() {
        assertEquals("Hoje", TaskUiMapper.ctaWhen(today, null, today))
        assertEquals("Amanhã, 09:00", TaskUiMapper.ctaWhen(today.plusDays(1), LocalTime.of(9, 0), today))
        assertEquals("Sex 13, 14:00", TaskUiMapper.ctaWhen(today.plusDays(3), LocalTime.of(14, 0), today))
    }

    private val tasks = listOf(
        Task(id = 1, title = "Revisar proposta", dueDate = today.minusDays(1), tags = listOf(Tag(1, "trabalho"))),
        Task(id = 2, title = "Ligar pro contador", dueDate = today, dueTime = LocalTime.of(17, 0), tags = listOf(Tag(2, "pessoal"))),
        Task(id = 3, title = "Organizar fotos", notes = "da viagem a Ouro Preto"),
        Task(id = 4, title = "Feito", dueDate = today, completedAt = Instant.EPOCH, tags = listOf(Tag(3, "antiga"))),
    )
    private val tags = listOf(Tag(3, "antiga"), Tag(2, "pessoal"), Tag(1, "trabalho"))

    @Test
    fun buildState_todas() {
        val s = TasksViewModel.buildState(tasks, tags, now, TaskFilter.All)
        assertEquals(listOf(TaskSection.Overdue, TaskSection.Today, TaskSection.NoDate), s.sections.map { it.section })
        assertEquals(listOf(4L), s.completed.map { it.id })
        assertEquals(1, s.overdueCount)
        assertEquals("só etiquetas de tarefas abertas viram filtro", listOf("pessoal", "trabalho"), s.tags.map { it.name })
        assertFalse(s.isEmpty)
        assertFalse(s.noTasksAtAll)
    }

    @Test
    fun buildState_buscaPorTituloNotaEEtiqueta() {
        fun ids(q: String) = TasksViewModel.buildState(tasks, tags, now, TaskFilter.All, query = q).sections.flatMap { it.rows }.map { it.id }
        assertEquals(listOf(2L), ids("contador"))
        assertEquals(listOf(3L), ids("ouro preto"))
        assertEquals(listOf(1L), ids("#trab"))
        assertTrue(TasksViewModel.buildState(tasks, tags, now, TaskFilter.All, query = "zzz").isEmpty)
    }

    @Test
    fun buildState_filtroDeEtiquetaInexistenteVoltaParaTodas_eVazio() {
        assertEquals(TaskFilter.All, TasksViewModel.buildState(tasks, tags, now, TaskFilter.ByTag(99)).filter)
        val empty = TasksViewModel.buildState(emptyList(), emptyList(), now, TaskFilter.All)
        assertTrue(empty.isEmpty)
        assertTrue(empty.noTasksAtAll)
    }
}
