package com.organizador.agenda.focus

import android.app.NotificationManager
import com.organizador.agenda.domain.focus.FocusMachine
import com.organizador.agenda.domain.focus.FocusState
import com.organizador.agenda.domain.model.FocusSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FocusDndTest {
    private val t0 = Instant.parse("2026-11-17T13:00:00Z")
    private val settings = FocusSettings()
    private val focusing = FocusMachine.start(null, null, settings, t0)

    // ---------- wanted ----------

    @Test
    fun wanted_soNoFocoComOpcaoEAcesso() {
        assertTrue(FocusDndRules.wanted(focusing, enabled = true, hasAccess = true))
        assertFalse("opção desligada (padrão)", FocusDndRules.wanted(focusing, enabled = false, hasAccess = true))
        assertFalse("sem acesso ao Não perturbe", FocusDndRules.wanted(focusing, enabled = true, hasAccess = false))
        assertFalse("sem sessão", FocusDndRules.wanted(FocusState(), enabled = true, hasAccess = true))
    }

    @Test
    fun wanted_focoPausadoContinua_pausaEFaseConcluidaNao() {
        val paused = FocusMachine.pause(focusing, t0.plusSeconds(60))
        assertTrue("foco pausado ainda é fase de foco", FocusDndRules.wanted(paused, true, true))

        val done = FocusMachine.elapse(focusing, t0.plusSeconds(25 * 60L))
        assertFalse("fase concluída: o aviso de fim precisa tocar", FocusDndRules.wanted(done, true, true))

        val shortBreak = FocusMachine.startBreak(done, settings, t0.plusSeconds(25 * 60L + 5))
        assertFalse("nas pausas as notificações voltam", FocusDndRules.wanted(shortBreak, true, true))

        val next = FocusMachine.nextFocus(shortBreak, settings, t0.plusSeconds(31 * 60L))
        assertTrue("o próximo foco liga de novo", FocusDndRules.wanted(next, true, true))

        assertFalse("encerrado", FocusDndRules.wanted(FocusMachine.stop(), true, true))
    }

    // ---------- Android 8/9 ----------

    @Test
    fun legado_soLigaSeEstavaEmTudo() {
        assertTrue(FocusDndRules.legacyShouldEnable(NotificationManager.INTERRUPTION_FILTER_ALL))
        assertFalse("Não perturbe do usuário já ligado", FocusDndRules.legacyShouldEnable(NotificationManager.INTERRUPTION_FILTER_PRIORITY))
        assertFalse(FocusDndRules.legacyShouldEnable(NotificationManager.INTERRUPTION_FILTER_NONE))
        assertFalse(FocusDndRules.legacyShouldEnable(NotificationManager.INTERRUPTION_FILTER_ALARMS))
    }

    @Test
    fun legado_soRestauraOQueLigamosESeAindaEstaComoDeixamos() {
        assertTrue(FocusDndRules.legacyShouldRestore(NotificationManager.INTERRUPTION_FILTER_PRIORITY, setByUs = true))
        assertFalse("o usuário já tinha ligado", FocusDndRules.legacyShouldRestore(NotificationManager.INTERRUPTION_FILTER_PRIORITY, setByUs = false))
        assertFalse("o usuário desligou no meio", FocusDndRules.legacyShouldRestore(NotificationManager.INTERRUPTION_FILTER_ALL, setByUs = true))
        assertFalse("o usuário trocou para silêncio total", FocusDndRules.legacyShouldRestore(NotificationManager.INTERRUPTION_FILTER_NONE, setByUs = true))
    }

    // ---------- shouldRelay ----------

    private val own = "com.organizador.agenda"
    private val allowed = setOf("com.whatsapp", own)
    private fun relay(n: FocusDndRules.Posted, active: Boolean = true) = FocusDndRules.shouldRelay(n, active, allowed, own)

    @Test
    fun repasse_appLiberadoComNaoPerturbeAtivo() {
        assertTrue(relay(FocusDndRules.Posted("com.whatsapp")))
        assertFalse("app não liberado", relay(FocusDndRules.Posted("com.instagram.android")))
        assertFalse("fora do foco", relay(FocusDndRules.Posted("com.whatsapp"), active = false))
    }

    @Test
    fun repasse_excluiContinuasServicoResumoVaziasProprioApp() {
        assertFalse("contínua", relay(FocusDndRules.Posted("com.whatsapp", ongoing = true)))
        assertFalse("serviço em primeiro plano", relay(FocusDndRules.Posted("com.whatsapp", foregroundService = true)))
        assertFalse("resumo de grupo", relay(FocusDndRules.Posted("com.whatsapp", groupSummary = true)))
        assertFalse("sem título nem texto", relay(FocusDndRules.Posted("com.whatsapp", hasContent = false)))
        assertFalse("do próprio Pulso (mesmo se estiver na lista)", relay(FocusDndRules.Posted(own)))
        assertFalse("já passou pelo Não perturbe: tocaria duas vezes", relay(FocusDndRules.Posted("com.whatsapp", intercepted = false)))
    }
}
