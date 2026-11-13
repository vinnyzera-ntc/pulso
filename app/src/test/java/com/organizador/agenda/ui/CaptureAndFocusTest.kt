package com.organizador.agenda.ui

import app.cash.turbine.test
import com.organizador.agenda.R
import com.organizador.agenda.domain.capture.CaptureKind
import com.organizador.agenda.domain.capture.CaptureParser
import com.organizador.agenda.domain.focus.FocusMachine
import com.organizador.agenda.domain.focus.FocusState
import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.FocusSettings
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.testing.FakeCalendarRepository
import com.organizador.agenda.testing.FakeNow
import com.organizador.agenda.testing.FakeSettingsRepository
import com.organizador.agenda.testing.FakeTaskRepository
import com.organizador.agenda.testing.deviceCalendar
import com.organizador.agenda.ui.capture.CaptureViewModel
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.focus.FocusViewModel
import com.organizador.agenda.ui.pulso.components.ChipTone
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureAndFocusTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val today = LocalDate.of(2026, 11, 10)
    private val now = FakeNow(LocalDateTime.of(today, LocalTime.of(10, 0)))
    private val tasks = FakeTaskRepository { now.instant() }
    private val settings = FakeSettingsRepository()
    private val messenger = Messenger(TestScope(dispatcher))

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun state(text: String, override: CaptureKind? = null, calendar: Boolean = true) =
        CaptureViewModel.buildState(text, override, CaptureParser.parse(text, now.now()), now.now(), calendar, emptyList())

    // ---------- Captura: estado ----------

    @Test
    fun captura_tarefa_chipsDeDataEtiquetaEPrioridade() {
        val s = state("ligar pro contador amanhã às 9h #pessoal p1")
        assertEquals(CaptureKind.Task, s.kind)
        assertEquals(listOf("Amanhã · 09:00", "P1", "#pessoal"), s.chips.map { it.text })
        assertEquals("data/hora em volt, o resto neutro", listOf(ChipTone.Volt, ChipTone.Neutral, ChipTone.Neutral), s.chips.map { it.tone })
        assertEquals(4, s.highlights.size)
        assertTrue(s.canSend)
        assertTrue("tem horário e agenda: dá para virar compromisso", s.canToggleKind)
    }

    @Test
    fun captura_compromisso_mostraDuracao_eTrocaManual() {
        val event = state("reunião com Ana sexta 14h por 1h")
        assertEquals(CaptureKind.Event, event.kind)
        assertEquals(listOf("Sex, 13 nov · 14:00", "1 h"), event.chips.map { it.text })
        // Sem "por 1h", a duração padrão aparece para conferência.
        assertEquals("1 h", state("consulta amanhã 15h").chips.last().text)
        // O usuário pode forçar tarefa ou compromisso.
        assertEquals(CaptureKind.Task, state("reunião com Ana sexta 14h por 1h", override = CaptureKind.Task).kind)
        assertEquals(CaptureKind.Event, state("ligar amanhã 9h", override = CaptureKind.Event).kind)
    }

    @Test
    fun captura_semHorarioOuSemAgenda_soTarefa() {
        val noTime = state("reunião amanhã", override = CaptureKind.Event)
        assertEquals(CaptureKind.Task, noTime.kind)
        assertFalse(noTime.canToggleKind)
        val noCalendar = state("reunião amanhã 14h por 1h", calendar = false)
        assertEquals(CaptureKind.Task, noCalendar.kind)
        assertFalse(noCalendar.canToggleKind)
    }

    @Test
    fun captura_vazioERepeticao() {
        assertFalse(state("").canSend)
        assertFalse(state("#casa").canSend)
        assertTrue(state("#casa").chips.isEmpty())
        val r = state("academia toda segunda 7h")
        assertFalse("tarefa repete de verdade", r.recurrenceIgnored)
        assertTrue(r.chips.any { it.text == "Toda segunda" })
        assertTrue(r.canSend)
        // Como compromisso, a repetição fica de fora (avisa).
        assertTrue(state("academia toda segunda 7h", override = CaptureKind.Event).recurrenceIgnored)
    }

    // ---------- Captura: criar ----------

    @Test
    fun captura_enviarTarefa_criaLimpaEGuardaNasRecentes() = runTest(dispatcher) {
        val vm = CaptureViewModel(tasks, FakeCalendarRepository(listOf(deviceCalendar(1))), settings, now, messenger)
        vm.state.test {
            vm.setText("ligar pro contador amanhã às 9h #pessoal p2")
            expectMostRecentItem()
            vm.send()
            val after = expectMostRecentItem()
            assertEquals("", after.text)
            assertEquals(listOf("ligar pro contador amanhã às 9h #pessoal p2"), after.recents)
        }
        val created = tasks.state.value.single()
        assertEquals("Ligar pro contador", created.title)
        assertEquals(today.plusDays(1), created.dueDate)
        assertEquals(LocalTime.of(9, 0), created.dueTime)
        assertEquals(Priority.P2, created.priority)
        assertEquals(listOf("pessoal"), created.tags.map { it.name })
        // Desfazer apaga a tarefa.
        val message = messenger.messages.first()
        assertEquals(R.string.msg_task_created, message.text)
        messenger.runAction(message)
        assertTrue(tasks.state.value.isEmpty())
    }

    @Test
    fun captura_falhaAoGravarNaAgenda_avisaEMantemOTexto() = runTest(dispatcher) {
        val calendar = FakeCalendarRepository(listOf(deviceCalendar(1, primary = true))).apply { failWrites = true }
        val vm = CaptureViewModel(tasks, calendar, settings, now, messenger)
        vm.state.test {
            vm.setText("reunião com Ana sexta 14h")
            expectMostRecentItem()
            vm.send()
            assertEquals("reunião com Ana sexta 14h", vm.state.value.text)
        }
        assertEquals(R.string.msg_calendar_write_failed, messenger.messages.first().text)
        assertTrue("não entra nas recentes", settings.settings.first().recentCaptures.isEmpty())
    }

    @Test
    fun captura_enviarCompromisso_criaNaAgendaPadraoComLembreteDosAjustes() = runTest(dispatcher) {
        val calendar = FakeCalendarRepository(listOf(deviceCalendar(1, write = false), deviceCalendar(2, primary = true)))
        settings.setEventReminderMinutes(30)
        val vm = CaptureViewModel(tasks, calendar, settings, now, messenger)
        vm.state.test {
            vm.setText("reunião com Ana sexta 14h por 1h30")
            assertEquals(CaptureKind.Event, expectMostRecentItem().kind)
            vm.send()
            expectMostRecentItem()
        }
        assertEquals(listOf("create"), calendar.writes)
        val draft = calendar.lastDraft!!
        assertEquals(2L, draft.calendarId)
        assertEquals("Reunião com Ana", draft.title)
        assertEquals(LocalDateTime.of(2026, 11, 13, 14, 0).atZone(now.zone).toInstant(), draft.start)
        assertEquals(90 * 60L, draft.end.epochSecond - draft.start.epochSecond)
        assertEquals(listOf(30), draft.reminderMinutes)
        assertTrue("nenhuma tarefa é criada", tasks.state.value.isEmpty())
        val message = messenger.messages.first()
        assertEquals(listOf("Sex, 13 nov · 14:00"), message.args)
        messenger.runAction(message)
        assertEquals(listOf("create", "delete:100"), calendar.writes)
    }

    @Test
    fun captura_trocarTipo_eVazioNaoEnvia() = runTest(dispatcher) {
        val calendar = FakeCalendarRepository(listOf(deviceCalendar(1)))
        val vm = CaptureViewModel(tasks, calendar, settings, now, messenger)
        vm.state.test {
            vm.send()
            vm.setText("ligar amanhã 9h")
            assertEquals(CaptureKind.Task, expectMostRecentItem().kind)
            vm.toggleKind()
            assertEquals(CaptureKind.Event, expectMostRecentItem().kind)
            vm.toggleKind()
            assertEquals(CaptureKind.Task, expectMostRecentItem().kind)
        }
        assertTrue(tasks.state.value.isEmpty() && calendar.writes.isEmpty())
    }

    // ---------- Foco: estado da tela ----------

    private val focusSettings = FocusSettings()
    private val t0 = now.instant()
    private val open = listOf(
        Task(id = 1, title = "Hoje sem hora", dueDate = today),
        Task(id = 2, title = "Atrasada", dueDate = today.minusDays(1)),
        Task(id = 3, title = "Semana que vem", dueDate = today.plusDays(7)),
        Task(id = 4, title = "Feita", dueDate = today, completedAt = t0),
    )

    private fun focusUi(focus: FocusState, at: java.time.Instant = t0, selected: Long? = null) =
        FocusViewModel.buildState(focus, at, open, now.now(), focusSettings, selected, false)

    @Test
    fun foco_preparacao_sugereTarefasAbertasDoDia_eAEscolhidaEntraPrimeiro() {
        val idle = focusUi(FocusState())
        assertFalse(idle.active)
        assertEquals(listOf("Atrasada", "Hoje sem hora"), idle.tasks.map { it.title })
        assertNull(idle.selectedTaskId)
        val picked = focusUi(FocusState(), selected = 3)
        assertEquals(listOf("Semana que vem", "Atrasada", "Hoje sem hora"), picked.tasks.map { it.title })
        assertEquals(3L, picked.selectedTaskId)
        // Tarefa concluída (ou que sumiu) deixa de estar escolhida.
        assertNull(focusUi(FocusState(), selected = 4).selectedTaskId)
    }

    @Test
    fun foco_sessao_tempoRestanteEProximaPausa() {
        val running = FocusMachine.start(1, "Hoje sem hora", focusSettings, t0)
        val ui = focusUi(running, at = t0.plusSeconds(61))
        assertTrue(ui.active && ui.running)
        assertEquals("23:59", ui.timeText)
        assertEquals("Hoje sem hora", ui.taskTitle)
        assertEquals(1, ui.session)
        assertEquals(FocusKind.ShortBreak, ui.nextBreak)
        assertEquals(5, ui.nextBreakMinutes)
        // Na 4ª sessão, a próxima pausa é a longa.
        val fourth = focusUi(running.copy(session = 4))
        assertEquals(FocusKind.LongBreak, fourth.nextBreak)
        assertEquals(15, fourth.nextBreakMinutes)
        val done = focusUi(FocusMachine.elapse(running, t0.plusSeconds(1500)), at = t0.plusSeconds(1600))
        assertTrue(done.finished)
        assertFalse(done.running)
        assertEquals("00:00", done.timeText)
    }
}
