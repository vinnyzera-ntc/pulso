package com.organizador.agenda.ui

import app.cash.turbine.test
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.Attendee
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Recurrence
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.task.DayProgress
import com.organizador.agenda.domain.task.TaskFilter
import com.organizador.agenda.testing.FakeCalendarRepository
import com.organizador.agenda.testing.FakeNow
import com.organizador.agenda.testing.FakeSettingsRepository
import com.organizador.agenda.testing.FakeTaskRepository
import com.organizador.agenda.testing.calendarEvent
import com.organizador.agenda.testing.deviceCalendar
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.tasks.NewTaskDraft
import com.organizador.agenda.ui.tasks.NewTaskViewModel
import com.organizador.agenda.ui.tasks.TasksViewModel
import com.organizador.agenda.ui.today.TodayViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 11, 10)
    private val now = FakeNow(LocalDateTime.of(today, LocalTime.of(10, 47)))
    private val tasks = FakeTaskRepository { now.instant() }
    private val messenger = Messenger(TestScope(dispatcher))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- Hoje ----------

    @Test
    fun tarefas_concluirTarefaQueSeRepete_vaiParaAProximaEDesfazVolta() = runTest(dispatcher) {
        val id = tasks.save(
            Task(
                title = "Pagar aluguel", dueDate = today.minusDays(5), recurrence = Recurrence.monthly(5),
                subtasks = listOf(com.organizador.agenda.domain.model.Subtask(title = "Pix", done = true)),
            ),
        )
        tasks.today = today
        val vm = TasksViewModel(tasks, now, messenger)
        vm.setDone(id, true)
        val t = tasks.getTask(id)!!
        assertFalse("não fica concluída", t.isDone)
        assertEquals(LocalDate.of(2026, 12, 5), t.dueDate)
        assertFalse("subtarefas desmarcadas", t.subtasks.single().done)
        val message = messenger.messages.first()
        assertEquals(R.string.msg_task_next, message.text)
        assertEquals(listOf("Sáb, 5 dez"), message.args)
        messenger.runAction(message)
        assertEquals(today.minusDays(5), tasks.getTask(id)!!.dueDate)
    }


    @Test
    fun hoje_progressoTarefasECompromissoDeAgora() = runTest(dispatcher) {
        tasks.save(Task(title = "Atrasada", dueDate = today.minusDays(1)))
        tasks.save(Task(title = "Às 14", dueDate = today, dueTime = LocalTime.of(14, 0)))
        val doneId = tasks.save(Task(title = "Feita", dueDate = today))
        tasks.setCompleted(doneId, true)
        tasks.save(Task(title = "Amanhã", dueDate = today.plusDays(1)))

        val start = now.instant().minus(47, ChronoUnit.MINUTES)
        val event = calendarEvent(5, start, start.plus(90, ChronoUnit.MINUTES), "Workshop", location = "Sala 3")
        val calendar = FakeCalendarRepository(listOf(deviceCalendar(1)), listOf(event))
        calendar.details = mapOf(5L to EventDetails(event, "", "UTC", listOf(Attendee("Ana Lima", "ana@x.com")), emptyList(), null))

        val vm = TodayViewModel(tasks, calendar, FakeSettingsRepository(), now, messenger)
        vm.state.test {
            val s = expectMostRecentItem()
            assertEquals("Terça · 10 nov", s.eyebrow)
            assertEquals("Bom dia", s.greeting)
            assertEquals(DayProgress(1, 3), s.progress)
            assertEquals(listOf("Atrasada", "Às 14", "Feita"), s.tasks.map { it.title })
            val card = s.event!!
            assertTrue(card.live)
            assertEquals("10:00 – 11:30", card.timeRange)
            assertEquals("termina em 43 min", card.relative)
            assertEquals("Sala 3", card.location)
            assertEquals(listOf("Ana Lima"), card.attendees)

            // O tempo passa: o evento acaba e não há próximo.
            now.current.value = LocalDateTime.of(today, LocalTime.of(11, 31))
            assertNull(expectMostRecentItem().event)
        }
    }

    @Test
    fun hoje_proximoCompromisso_eAgendaOcultaNaoAparece() = runTest(dispatcher) {
        val start = now.instant().plus(25, ChronoUnit.MINUTES)
        val calendar = FakeCalendarRepository(
            listOf(deviceCalendar(1), deviceCalendar(2)),
            listOf(calendarEvent(1, start, start.plusSeconds(3600), "Almoço"), calendarEvent(2, now.instant().minusSeconds(60), start, "Oculto", calendarId = 2)),
        )
        val settings = FakeSettingsRepository().apply { setCalendarVisible(2, false) }
        val vm = TodayViewModel(tasks, calendar, settings, now, messenger)
        vm.state.test {
            val card = expectMostRecentItem().event!!
            assertFalse(card.live)
            assertEquals("Almoço", card.title)
            assertEquals("começa em 25 min", card.relative)
        }
    }

    @Test
    fun hoje_semPermissao_eConcluirTarefa() = runTest(dispatcher) {
        val id = tasks.save(Task(title = "A", dueDate = today))
        val vm = TodayViewModel(tasks, FakeCalendarRepository(permission = false), FakeSettingsRepository(), now, messenger)
        vm.state.test {
            val s = expectMostRecentItem()
            assertFalse(s.calendarConnected)
            assertNull(s.event)
            vm.setDone(id, true)
            val after = expectMostRecentItem()
            assertEquals(DayProgress(1, 1), after.progress)
            assertTrue(after.tasks.single().done)
        }
    }

    // ---------- Tarefas ----------

    @Test
    fun tarefas_concluirMoveParaConcluidasEDesfazerReabre() = runTest(dispatcher) {
        val id = tasks.save(Task(title = "A", dueDate = today, priority = Priority.P1))
        val vm = TasksViewModel(tasks, now, messenger)
        vm.state.test {
            assertEquals(1, expectMostRecentItem().sections.single().rows.size)
            vm.setDone(id, true)
            val s = expectMostRecentItem()
            assertTrue(s.sections.isEmpty())
            assertEquals(listOf(id), s.completed.map { it.id })

            val message = messenger.messages.first()
            assertEquals(R.string.msg_task_completed, message.text)
            messenger.runAction(message)
            assertEquals(1, expectMostRecentItem().sections.single().rows.size)
        }
    }

    @Test
    fun tarefas_filtroEBusca() = runTest(dispatcher) {
        tasks.save(Task(title = "Hoje", dueDate = today))
        tasks.save(Task(title = "Sem data"))
        val vm = TasksViewModel(tasks, now, messenger)
        vm.state.test {
            vm.setFilter(TaskFilter.NoDate)
            assertEquals(listOf("Sem data"), expectMostRecentItem().sections.flatMap { it.rows }.map { it.title })
            vm.setFilter(TaskFilter.All)
            vm.setSearching(true)
            vm.setQuery("hoj")
            assertEquals(listOf("Hoje"), expectMostRecentItem().sections.flatMap { it.rows }.map { it.title })
            vm.setSearching(false)
            val s = expectMostRecentItem()
            assertEquals("", s.query)
            assertEquals(2, s.sections.sumOf { it.rows.size })
        }
    }

    // ---------- Nova tarefa ----------

    @Test
    fun novaTarefa_criaComLembreteEtiquetasEDesfazerExclui() = runTest(dispatcher) {
        val vm = NewTaskViewModel(tasks, now, messenger)
        assertEquals(today, vm.today())
        vm.create(NewTaskDraft(" Ligar pro contador ", today.plusDays(1), LocalTime.of(9, 0), Priority.P2, 10, setOf("pessoal")))
        val created = tasks.state.value.single()
        assertEquals("Ligar pro contador", created.title)
        assertEquals(LocalTime.of(9, 0), created.dueTime)
        assertEquals(listOf(10), created.reminders.map { it.offsetMinutes })
        assertEquals(listOf("pessoal"), created.tags.map { it.name })

        messenger.runAction(messenger.messages.first())
        assertTrue(tasks.state.value.isEmpty())
    }

    @Test
    fun novaTarefa_semDataDescartaHoraELembrete_eTituloVazioNaoCria() = runTest(dispatcher) {
        val task = NewTaskDraft("X", date = null, time = LocalTime.NOON, reminderMinutes = 10).toTask()
        assertNull(task.dueTime)
        assertTrue(task.reminders.isEmpty())

        NewTaskViewModel(tasks, now, messenger).create(NewTaskDraft("   "))
        assertTrue(tasks.state.value.isEmpty())
    }
}
