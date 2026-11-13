package com.organizador.agenda.domain.reminder

import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.model.TaskReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val nine = LocalTime.of(9, 0)
    private val day = LocalDate.of(2026, 11, 12)

    private fun at(h: Int, m: Int = 0, d: LocalDate = day): Instant = d.atTime(h, m).atZone(zone).toInstant()

    private fun task(
        id: Long,
        time: LocalTime? = LocalTime.of(14, 0),
        date: LocalDate? = day,
        vararg reminders: TaskReminder,
    ) = Task(id = id, title = "T$id", dueDate = date, dueTime = time, reminders = reminders.toList())

    private fun plan(tasks: List<Task>, since: Instant, now: Instant) = ReminderPlanner.plan(tasks, nine, zone, since, now)

    @Test
    fun venceuDesdeAUltimaConferencia_notifica() {
        val t = task(1, reminders = arrayOf(TaskReminder(id = 10, offsetMinutes = 15)))
        val p = plan(listOf(t), since = at(13, 40), now = at(13, 45))
        assertEquals(listOf(10L), p.due.single().reminderIds)
        assertNull(p.next)
    }

    @Test
    fun futuro_viraProximoAlarme() {
        val t = task(1, reminders = arrayOf(TaskReminder(id = 10, offsetMinutes = 0), TaskReminder(id = 11, offsetMinutes = 60)))
        val p = plan(listOf(t), since = at(12, 0), now = at(12, 0))
        assertTrue(p.due.isEmpty())
        assertEquals(at(13, 0), p.next)
    }

    @Test
    fun horarioJaPassadoAntesDaUltimaConferencia_naoNotifica() {
        // Tarefa criada às 15h para hoje sem hora: o lembrete das 09:00 não toca do nada.
        val t = task(1, time = null, reminders = arrayOf(TaskReminder(id = 10)))
        val p = plan(listOf(t), since = at(15, 0), now = at(15, 0))
        assertTrue(p.due.isEmpty())
        assertNull(p.next)
    }

    @Test
    fun edicaoComAppAbertoHaHoras_horarioPassadoNaoToca() {
        // App aberto desde as 08:00 sem conferir; às 15:00 cria tarefa de hoje sem hora (lembrete 09:00).
        val since = ReminderPlanner.since(stored = at(8, 0), now = at(15, 0), fromEdit = true)
        val t = task(1, time = null, reminders = arrayOf(TaskReminder(id = 10)))
        assertTrue(plan(listOf(t), since, at(15, 0)).due.isEmpty())
    }

    @Test
    fun since_alarmeEReligarUsamOGravado_relogioVoltandoNaoEscondeNada() {
        assertEquals(at(8, 0), ReminderPlanner.since(at(8, 0), at(15, 0), fromEdit = false))
        assertEquals(at(14, 59), ReminderPlanner.since(at(8, 0), at(15, 0), fromEdit = true))
        assertEquals(at(15, 0), ReminderPlanner.since(null, at(15, 0), fromEdit = false))
        assertEquals("relógio voltou", at(10, 0), ReminderPlanner.since(at(12, 0), at(10, 0), fromEdit = false))
    }

    @Test
    fun perdidoComAparelhoDesligado_tocaAoReligar() {
        val t = task(1, reminders = arrayOf(TaskReminder(id = 10)))
        val p = plan(listOf(t), since = at(8, 0), now = at(18, 0))
        assertEquals(1, p.due.size)
    }

    @Test
    fun jaDisparado_naoRepete() {
        val t = task(1, reminders = arrayOf(TaskReminder(id = 10, firedAt = at(14, 0))))
        assertTrue(plan(listOf(t), since = at(13, 0), now = at(14, 1)).due.isEmpty())
    }

    @Test
    fun disparadoMasTarefaMovidaParaAmanha_valeDeNovo() {
        val tomorrow = day.plusDays(1)
        val t = task(1, date = tomorrow, reminders = arrayOf(TaskReminder(id = 10, firedAt = at(14, 0))))
        assertEquals(at(14, 0, tomorrow), plan(listOf(t), since = at(15, 0), now = at(15, 0)).next)
    }

    @Test
    fun adiado_usaHorarioDoAdiamento() {
        val snooze = LocalDateTime.of(day, LocalTime.of(14, 10))
        val t = task(1, reminders = arrayOf(TaskReminder(id = 10, snoozedUntil = snooze, firedAt = at(14, 0))))
        assertEquals(at(14, 10), plan(listOf(t), since = at(14, 0), now = at(14, 0)).next)
        assertEquals(1, plan(listOf(t), since = at(14, 5), now = at(14, 10)).due.size)
    }

    @Test
    fun concluidaOuSemData_ignoradas() {
        val done = task(1, reminders = arrayOf(TaskReminder(id = 10))).copy(completedAt = at(10, 0))
        val noDate = task(2, date = null, reminders = arrayOf(TaskReminder(id = 20)))
        val p = plan(listOf(done, noDate), since = at(0, 0), now = at(20, 0))
        assertTrue(p.due.isEmpty())
        assertNull(p.next)
    }

    @Test
    fun variosDaMesmaTarefa_umaNotificacaoComOMaisRecente() {
        val t = task(1, reminders = arrayOf(TaskReminder(id = 10, offsetMinutes = 30), TaskReminder(id = 11, offsetMinutes = 0)))
        val d = plan(listOf(t), since = at(13, 0), now = at(14, 0)).due.single()
        assertEquals(setOf(10L, 11L), d.reminderIds.toSet())
        assertEquals(11L, d.latestReminderId)
    }

    @Test
    fun proximoEhOMaisCedoEntreTarefas() {
        val a = task(1, time = LocalTime.of(16, 0), reminders = arrayOf(TaskReminder(id = 10)))
        val b = task(2, time = LocalTime.of(15, 0), reminders = arrayOf(TaskReminder(id = 20)))
        assertEquals(at(15, 0), plan(listOf(a, b), since = at(12, 0), now = at(12, 0)).next)
    }

    @Test
    fun trocaDeFuso_recalculaPeloHorarioLocal() {
        val t = task(1, reminders = arrayOf(TaskReminder(id = 10)))
        val lisbon = ZoneId.of("Europe/Lisbon")
        val now = day.atTime(9, 0).atZone(lisbon).toInstant()
        val p = ReminderPlanner.plan(listOf(t), nine, lisbon, now, now)
        assertEquals(day.atTime(14, 0).atZone(lisbon).toInstant(), p.next)
    }
}
