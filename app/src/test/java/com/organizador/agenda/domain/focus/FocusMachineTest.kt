package com.organizador.agenda.domain.focus

import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.FocusSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FocusMachineTest {
    private val t0 = Instant.parse("2026-11-10T13:00:00Z")
    private val settings = FocusSettings(focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15, sessionsBeforeLongBreak = 4)
    private fun at(seconds: Long) = t0.plusSeconds(seconds)

    @Test
    fun iniciar_contaATerDoRelogio() {
        val s = FocusMachine.start(7, "Relatório", settings, t0)
        assertTrue(s.active && s.running)
        assertEquals(FocusKind.Focus, s.phase)
        assertEquals(1, s.session)
        assertEquals(1500, s.totalSeconds)
        assertEquals(1500, s.remainingSeconds(t0))
        // 10 minutos depois, sem nenhum "tique" no meio: o restante sai do relógio.
        assertEquals(900, s.remainingSeconds(at(600)))
        assertEquals(0.4f, s.progress(at(600)), 0.001f)
        assertEquals(0, s.remainingSeconds(at(5000)))
    }

    @Test
    fun pausarERetomar_preservaORestante() {
        val running = FocusMachine.start(null, null, settings, t0)
        val paused = FocusMachine.pause(running, at(300))
        assertTrue(paused.paused)
        assertFalse(paused.running)
        assertEquals(1200, paused.remainingSeconds(at(9999)))
        val resumed = FocusMachine.resume(paused, at(1000))
        assertEquals(at(2200), resumed.endsAt)
        assertEquals(1200, resumed.remainingSeconds(at(1000)))
        // Operações fora de hora não mudam nada.
        assertSame(paused, FocusMachine.pause(paused, at(400)))
        assertSame(running, FocusMachine.resume(running, at(400)))
    }

    @Test
    fun elapse_soTerminaQuandoOFimChegou() {
        val s = FocusMachine.start(null, null, settings, t0)
        assertSame(s, FocusMachine.elapse(s, at(1499)))
        val done = FocusMachine.elapse(s, at(1500))
        assertTrue(done.finished && done.active)
        assertFalse(done.running)
        assertFalse(done.paused)
        assertEquals(0, done.remainingSeconds(at(2000)))
        // Um alarme atrasado ou repetido não faz nada.
        assertSame(done, FocusMachine.elapse(done, at(3000)))
        val paused = FocusMachine.pause(s, at(100))
        assertSame(paused, FocusMachine.elapse(paused, at(9000)))
    }

    @Test
    fun cicloCompleto_pausaCurtaAteAQuartaSessaoDepoisLonga() {
        var s = FocusMachine.start(1, "T", settings, t0)
        val phases = mutableListOf<String>()
        var now = t0
        repeat(4) {
            phases += "foco ${s.session}"
            now = s.endsAt!!
            s = FocusMachine.elapse(s, now)
            s = FocusMachine.startBreak(s, settings, now)
            phases += "${s.phase} ${s.totalSeconds / 60}"
            now = s.endsAt!!
            s = FocusMachine.elapse(s, now)
            s = FocusMachine.nextFocus(s, settings, now)
        }
        assertEquals(
            listOf("foco 1", "ShortBreak 5", "foco 2", "ShortBreak 5", "foco 3", "ShortBreak 5", "foco 4", "LongBreak 15"),
            phases,
        )
        // Depois da pausa longa, o ciclo recomeça na sessão 1, com a mesma tarefa.
        assertEquals(1, s.session)
        assertEquals(FocusKind.Focus, s.phase)
        assertEquals(1L, s.taskId)
    }

    @Test
    fun proximaSessao_pulaAPausa() {
        val done = FocusMachine.elapse(FocusMachine.start(null, null, settings, t0), at(1500))
        val next = FocusMachine.nextFocus(done, settings, at(1500))
        assertEquals(2, next.session)
        assertEquals(FocusKind.Focus, next.phase)
        assertTrue(next.running)
    }

    @Test
    fun ajustesDiferentes_valemNaFaseSeguinte() {
        val s = FocusMachine.start(null, null, settings, t0)
        val custom = settings.copy(shortBreakMinutes = 10, sessionsBeforeLongBreak = 2)
        val b = FocusMachine.startBreak(FocusMachine.elapse(s, at(1500)), custom, at(1500))
        assertEquals(600, b.totalSeconds)
        assertEquals(2, b.sessionsPerCycle)
    }

    @Test
    fun pararEFormatar() {
        val stopped = FocusMachine.stop()
        assertFalse(stopped.active)
        assertNull(stopped.endsAt)
        assertEquals("25:00", FocusMachine.format(1500))
        assertEquals("00:07", FocusMachine.format(7))
        assertEquals("1:05:00", FocusMachine.format(3900))
        assertEquals("00:00", FocusMachine.format(-5))
    }
}
