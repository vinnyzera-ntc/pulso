package com.organizador.agenda.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class TaskReminderTest {
    private val nine = LocalTime.of(9, 0)
    private val day = LocalDate.of(2026, 11, 10)

    @Test
    fun comHora_subtraiOffset() {
        val task = Task(title = "Ligar", dueDate = day, dueTime = LocalTime.of(14, 0))
        assertEquals(LocalDateTime.of(2026, 11, 10, 13, 45), TaskReminder(offsetMinutes = 15).triggerAt(task, nine))
    }

    @Test
    fun semHora_usaHorarioPadrao() {
        val task = Task(title = "Pagar boleto", dueDate = day)
        assertEquals(LocalDateTime.of(2026, 11, 10, 9, 0), TaskReminder(offsetMinutes = 0).triggerAt(task, nine))
    }

    @Test
    fun offsetCruzaMeiaNoite() {
        val task = Task(title = "Cedo", dueDate = day, dueTime = LocalTime.of(0, 30))
        assertEquals(LocalDateTime.of(2026, 11, 9, 23, 30), TaskReminder(offsetMinutes = 60).triggerAt(task, nine))
    }

    @Test
    fun adiado_ignoraCalculo() {
        val task = Task(title = "Ligar", dueDate = day, dueTime = LocalTime.of(14, 0))
        val snooze = LocalDateTime.of(2026, 11, 10, 15, 0)
        assertEquals(snooze, TaskReminder(offsetMinutes = 15, snoozedUntil = snooze).triggerAt(task, nine))
    }

    @Test
    fun semData_naoTemLembrete() {
        assertNull(TaskReminder().triggerAt(Task(title = "Algum dia"), nine))
    }

    @Test
    fun priorityOf_valorInvalidoViraP4() {
        assertEquals(Priority.P1, Priority.of(1))
        assertEquals(Priority.P4, Priority.of(0))
        assertEquals(Priority.P4, Priority.of(9))
    }
}
