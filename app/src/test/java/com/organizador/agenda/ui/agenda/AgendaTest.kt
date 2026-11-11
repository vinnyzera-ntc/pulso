package com.organizador.agenda.ui.agenda

import com.organizador.agenda.R
import com.organizador.agenda.data.calendar.PendingEventChanges
import com.organizador.agenda.domain.calendar.EventForm
import com.organizador.agenda.domain.calendar.EventSummary
import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.Attendee
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.testing.FakeCalendarRepository
import com.organizador.agenda.testing.FakeNow
import com.organizador.agenda.testing.FakeSettingsRepository
import com.organizador.agenda.testing.TEST_ZONE
import com.organizador.agenda.testing.calendarEvent
import com.organizador.agenda.testing.deviceCalendar
import com.organizador.agenda.ui.common.Messenger
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val day = LocalDate.of(2026, 11, 10) // terça
    private val now = FakeNow(LocalDateTime.of(day, LocalTime.of(10, 47)))
    private fun at(h: Int, m: Int = 0, d: LocalDate = day) = d.atTime(h, m).atZone(TEST_ZONE).toInstant()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    // ---------- Formulário ----------

    @Test
    fun form_inicioSugeridoEAProximaMeiaHora() {
        assertEquals(LocalDateTime.of(day, LocalTime.of(11, 0)), EventForm.suggestedStart(LocalDateTime.of(day, LocalTime.of(10, 47, 30))))
        assertEquals(LocalDateTime.of(day, LocalTime.of(10, 30)), EventForm.suggestedStart(LocalDateTime.of(day, LocalTime.of(10, 0))))
        assertEquals(day.plusDays(1).atStartOfDay(), EventForm.suggestedStart(LocalDateTime.of(day, LocalTime.of(23, 40))))
    }

    @Test
    fun form_validacaoERascunho() {
        val form = EventForm(title = " Dentista ", calendarId = 3, date = day, time = LocalTime.of(14, 0), durationMinutes = 45, location = " Clínica ", reminderMinutes = 30)
        assertTrue(form.isValid)
        assertFalse(form.copy(title = "  ").isValid)
        assertFalse(form.copy(calendarId = null).isValid)
        assertFalse(form.copy(durationMinutes = 0).isValid)
        val draft = form.toDraft(TEST_ZONE)
        assertEquals("Dentista", draft.title)
        assertEquals("Clínica", draft.location)
        assertEquals(at(14), draft.start)
        assertEquals(at(14, 45), draft.end)
        assertEquals(listOf(30), draft.reminderMinutes)
        assertEquals(3L, draft.calendarId)
    }

    @Test
    fun form_editarPreservaParticipantesELembretesExtras() {
        val details = EventDetails(
            event = calendarEvent(7, at(9), at(10), "Standup", calendarId = 2, location = "Sala 1"),
            description = "pauta", timeZone = "UTC",
            attendees = listOf(Attendee("Ana Lima", "ana@x.com")), reminderMinutes = listOf(60, 10), rrule = null,
        )
        // Ocorrência das 14h às 15h30 (pode diferir do evento-mestre numa série).
        val form = EventForm.edit(details, at(14), at(15, 30), TEST_ZONE)
        assertEquals(LocalTime.of(14, 0), form.time)
        assertEquals(90, form.durationMinutes)
        assertEquals(10, form.reminderMinutes)
        assertEquals(listOf(60), form.extraReminders)
        val draft = form.copy(reminderMinutes = null).toDraft(TEST_ZONE)
        assertEquals("tirar o lembrete mostrado mantém o outro", listOf(60), draft.reminderMinutes)
        assertEquals(listOf("ana@x.com"), draft.attendees.map { it.email })
        assertEquals("pauta", draft.description)
    }

    @Test
    fun summary_textosDaConfirmacao() {
        val form = EventForm(title = "Dentista", calendarId = 1, date = day, time = LocalTime.of(14, 0), durationMinutes = 45, location = "Clínica", reminderMinutes = 10)
        assertEquals("Ter, 10 nov · 14:00", EventSummary.value(form))
        assertEquals("45 min · Clínica", EventSummary.meta(form))
        assertEquals("Lembrete 10 min antes", EventSummary.reminder(form))
        assertNull(EventSummary.reminder(form.copy(reminderMinutes = null)))
        assertEquals("1 h 30 min", EventSummary.meta(form.copy(durationMinutes = 90, location = "")))
        val people = listOf(Attendee("Ana Lima", "a@x"), Attendee("Bruno Sá", "b@x"), Attendee("c@x.com", "c@x.com"))
        assertEquals("Com Ana, Bruno e mais 1 · 45 min · Clínica", EventSummary.meta(form.copy(attendees = people)))
    }

    // ---------- Estado da Agenda ----------

    private val calendars = listOf(deviceCalendar(1), deviceCalendar(2, write = false))
    private val events = listOf(
        calendarEvent(1, at(10), at(11, 30), "Workshop", location = "Sala 3"),
        calendarEvent(2, at(11), at(12), "Somente leitura", calendarId = 2),
        calendarEvent(3, at(15), at(16), "Dentista"),
        calendarEvent(4, at(9, 0, day.plusDays(1)), at(10, 0, day.plusDays(1)), "Amanhã"),
    )

    private fun state(mode: AgendaMode = AgendaMode.Week, selected: LocalDate = day, weekStart: DayOfWeek = DayOfWeek.SUNDAY) =
        AgendaViewModel.buildState(mode, selected, now.now(), TEST_ZONE, AppSettings(weekStart = weekStart), calendars, events, hasAccess = true)

    @Test
    fun estado_blocosDoDiaComColunasAoVivoEPermissaoDeEscrita() {
        val s = state()
        assertEquals(listOf("Workshop", "Somente leitura", "Dentista"), s.blocks.map { it.title })
        val (workshop, readOnly, dentist) = s.blocks
        assertEquals(600, workshop.startMin)
        assertEquals(690, workshop.endMin)
        assertEquals("10:00 – 11:30", workshop.timeRange)
        assertTrue(workshop.live)
        assertFalse(readOnly.live)
        assertEquals(listOf(0 to 2, 1 to 2, 0 to 1), s.blocks.map { it.column to it.columns })
        assertTrue(workshop.canEdit)
        assertFalse(readOnly.canEdit)
        assertEquals(647, s.nowMinutes)
        assertEquals(mapOf(day to 3, day.plusDays(1) to 1), s.eventCounts)
        assertEquals(dentist.eventId, 3L)
    }

    @Test
    fun estado_cabecalhoENavegadorPorVisao() {
        val week = state()
        assertEquals("Semana 46", week.eyebrow)
        assertEquals("Novembro", week.title)
        assertEquals("8 – 14 nov", week.navigatorLabel)
        assertEquals(LocalDate.of(2026, 11, 8), week.weekDays.first())
        assertEquals("Ter, 10 nov", state(AgendaMode.Day).navigatorLabel)
        val month = state(AgendaMode.Month)
        assertEquals("Novembro 2026", month.navigatorLabel)
        assertEquals(5, month.monthWeeks.size)
        assertEquals("9 – 15 nov", state(weekStart = DayOfWeek.MONDAY).navigatorLabel)
        // Outro dia: sem linha do "agora"; semana que cruza o mês; ano diferente aparece no título.
        val other = state(selected = LocalDate.of(2026, 12, 1))
        assertNull(other.nowMinutes)
        assertEquals("29 nov – 5 dez", other.navigatorLabel)
        assertEquals("Janeiro 2027", state(selected = LocalDate.of(2027, 1, 5)).title)
    }

    // ---------- Remarcar e cancelar ----------

    private val calendar = FakeCalendarRepository(calendars, events).apply {
        details = this@AgendaTest.events.associate { it.eventId to EventDetails(it, "", "UTC", emptyList(), listOf(10), null) }
    }
    private val pending = PendingEventChanges()
    private val messenger = Messenger(TestScope(dispatcher))
    private val editor = EventEditor(calendar, pending, messenger, now)
    private fun target(scope: EditScope) = EventTarget(3, 3, at(15), at(16), scope)

    @Test
    fun remarcarAvulso_mantemDuracaoEDesfazerVoltaAoHorario() = runTest(dispatcher) {
        editor.move(target(EditScope.Single), at(16, 45))
        assertEquals(listOf("update:3"), calendar.writes)
        assertEquals(at(16, 45), calendar.lastDraft!!.start)
        assertEquals(at(17, 45), calendar.lastDraft!!.end)
        assertEquals("lembrete preservado", listOf(10), calendar.lastDraft!!.reminderMinutes)

        val message = messenger.messages.first()
        assertEquals(R.string.msg_event_moved, message.text)
        assertEquals(listOf("16:45"), message.args)
        messenger.runAction(message)
        assertEquals(listOf("update:3", "update:3"), calendar.writes)
        assertEquals(at(15), calendar.lastDraft!!.start)
    }

    @Test
    fun remarcarRecorrente_usaOAlcanceEscolhidoESemDesfazer() = runTest(dispatcher) {
        editor.move(target(EditScope.Occurrence), at(17))
        assertEquals(listOf("updateOccurrence:3"), calendar.writes)
        assertNull(messenger.messages.first().actionLabel)
        editor.move(target(EditScope.Series), at(18))
        assertEquals(listOf("updateOccurrence:3", "updateSeries:3"), calendar.writes)
    }

    @Test
    fun cancelar_someNaHora_soApagaQuandoOAvisoExpira() = runTest(dispatcher) {
        editor.cancel(target(EditScope.Single))
        assertTrue(pending.hidden.value.hides(3, 3))
        assertTrue("nada é apagado enquanto dá para desfazer", calendar.writes.isEmpty())
        messenger.runDismissed(messenger.messages.first())
        assertEquals(listOf("delete:3"), calendar.writes)
    }

    @Test
    fun cancelar_desfazerNaoEscreveNadaNaAgenda() = runTest(dispatcher) {
        editor.cancel(target(EditScope.Series))
        messenger.runAction(messenger.messages.first())
        assertFalse(pending.hidden.value.hides(3, 3))
        assertTrue(calendar.writes.isEmpty())
    }

    @Test
    fun cancelarSoEsteDeUmaSerie_escondeAOcorrenciaECriaExcecao() = runTest(dispatcher) {
        editor.cancel(EventTarget(3, 99, at(15), at(16), EditScope.Occurrence))
        assertTrue(pending.hidden.value.hides(3, 99))
        assertFalse("as outras ocorrências continuam visíveis", pending.hidden.value.hides(3, 100))
        messenger.runDismissed(messenger.messages.first())
        assertEquals(listOf("cancelOccurrence:3"), calendar.writes)
    }

    @Test
    fun salvar_novoEditarESerie() = runTest(dispatcher) {
        val draft = EventForm(title = "X", calendarId = 1, date = day, time = LocalTime.NOON).toDraft(TEST_ZONE)
        editor.save(null, draft)
        editor.save(target(EditScope.Single), draft)
        editor.save(target(EditScope.Occurrence), draft)
        editor.save(target(EditScope.Series), draft)
        assertEquals(listOf("create", "update:3", "updateOccurrence:3", "updateSeries:3"), calendar.writes)
    }

    @Test
    fun formularioViewModel_novoUsaAgendaPadraoELembreteDosAjustes_eConfirma() = runTest(dispatcher) {
        val settings = FakeSettingsRepository().apply { setEventReminderMinutes(30) }
        val vm = EventFormViewModel(calendar, settings, now, editor)
        vm.start(null, null)
        val s = vm.state.value
        assertTrue(s.ready)
        assertFalse(s.editing)
        assertEquals(1L, s.form!!.calendarId)
        assertEquals(LocalTime.of(11, 0), s.form!!.time)
        assertEquals(30, s.form!!.reminderMinutes)
        assertEquals("só agendas com escrita", listOf(1L), s.calendars.map { it.id })

        vm.save()
        assertTrue("título vazio não grava", calendar.writes.isEmpty())
        vm.update { it.copy(title = "Dentista", durationMinutes = 45) }
        vm.save()
        assertEquals(listOf("create"), calendar.writes)
        val c = vm.state.value.confirmation!!
        assertTrue(c.created)
        assertEquals("Ter, 10 nov · 11:00", c.value)
        assertEquals("45 min", c.meta)
        assertEquals("Lembrete 30 min antes", c.reminder)
    }

    @Test
    fun formularioViewModel_editarCarregaOEventoESemPermissaoNaoCarrega() = runTest(dispatcher) {
        val vm = EventFormViewModel(calendar, FakeSettingsRepository(), now, editor)
        vm.start(target(EditScope.Single), null)
        assertTrue(vm.state.value.editing)
        assertEquals("Dentista", vm.state.value.form!!.title)
        assertEquals(60, vm.state.value.form!!.durationMinutes)
        vm.update { it.copy(time = LocalTime.of(16, 0)) }
        vm.save()
        assertEquals(listOf("update:3"), calendar.writes)
        assertFalse(vm.state.value.confirmation!!.created)

        val denied = EventFormViewModel(FakeCalendarRepository(permission = false), FakeSettingsRepository(), now, editor)
        denied.start(null, null)
        assertTrue(denied.state.value.ready)
        assertFalse(denied.state.value.hasCalendarAccess)
        assertNull(denied.state.value.form)
    }
}
