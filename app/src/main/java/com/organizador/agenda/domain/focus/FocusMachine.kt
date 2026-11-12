package com.organizador.agenda.domain.focus

import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.FocusSettings
import java.time.Duration
import java.time.Instant

/**
 * Estado do modo foco. Enquanto roda, o fim é um instante ([endsAt]); o tempo restante é sempre
 * calculado a partir do relógio, então continua certo com a tela apagada ou o app em segundo plano.
 */
data class FocusState(
    val active: Boolean = false,
    val phase: FocusKind = FocusKind.Focus,
    val taskId: Long? = null,
    val taskTitle: String? = null,
    /** Sessão de foco atual dentro do ciclo, a partir de 1 (durante uma pausa, a que acabou). */
    val session: Int = 1,
    val sessionsPerCycle: Int = 4,
    val totalSeconds: Long = 0,
    /** Preenchido só enquanto o timer corre. */
    val endsAt: Instant? = null,
    /** Tempo restante quando pausado (ou zero quando a fase terminou). */
    val pausedRemaining: Long = 0,
    /** A fase chegou ao fim e espera a escolha do usuário (pausa, próxima sessão ou encerrar). */
    val finished: Boolean = false,
) {
    val running: Boolean get() = active && endsAt != null
    val paused: Boolean get() = active && endsAt == null && !finished

    fun remainingSeconds(now: Instant): Long =
        endsAt?.let { Duration.between(now, it).seconds.coerceAtLeast(0) } ?: pausedRemaining

    /** Fração decorrida da fase (0..1). */
    fun progress(now: Instant): Float =
        if (totalSeconds <= 0) 0f else (1f - remainingSeconds(now).toFloat() / totalSeconds).coerceIn(0f, 1f)
}

/** Transições do Pomodoro. Puras: recebem o estado e o "agora" e devolvem o novo estado. */
object FocusMachine {

    fun minutesOf(phase: FocusKind, settings: FocusSettings): Int = when (phase) {
        FocusKind.Focus -> settings.focusMinutes
        FocusKind.ShortBreak -> settings.shortBreakMinutes
        FocusKind.LongBreak -> settings.longBreakMinutes
    }

    private fun begin(base: FocusState, phase: FocusKind, session: Int, settings: FocusSettings, now: Instant): FocusState {
        val seconds = minutesOf(phase, settings) * 60L
        return base.copy(
            active = true, phase = phase, session = session, sessionsPerCycle = settings.sessionsBeforeLongBreak,
            totalSeconds = seconds, endsAt = now.plusSeconds(seconds), pausedRemaining = 0, finished = false,
        )
    }

    /** Começa a primeira sessão de foco, ligada (ou não) a uma tarefa. */
    fun start(taskId: Long?, taskTitle: String?, settings: FocusSettings, now: Instant): FocusState =
        begin(FocusState(taskId = taskId, taskTitle = taskTitle), FocusKind.Focus, 1, settings, now)

    fun pause(state: FocusState, now: Instant): FocusState =
        if (!state.running) state else state.copy(endsAt = null, pausedRemaining = state.remainingSeconds(now))

    fun resume(state: FocusState, now: Instant): FocusState =
        if (!state.paused) state else state.copy(endsAt = now.plusSeconds(state.pausedRemaining), pausedRemaining = 0)

    /** O tempo da fase acabou. Só vale se estava correndo e o fim já chegou. */
    fun elapse(state: FocusState, now: Instant): FocusState {
        val end = state.endsAt ?: return state
        return if (now < end) state else state.copy(endsAt = null, pausedRemaining = 0, finished = true)
    }

    /** A pausa que vem depois da sessão de foco atual: longa ao fechar o ciclo. */
    fun breakAfter(state: FocusState): FocusKind =
        if (state.session >= state.sessionsPerCycle) FocusKind.LongBreak else FocusKind.ShortBreak

    /** Depois de um foco: começa a pausa devida. Depois de uma pausa: começa o próximo foco. */
    fun startBreak(state: FocusState, settings: FocusSettings, now: Instant): FocusState =
        begin(state, breakAfter(state), state.session, settings, now)

    /** Próxima sessão de foco (pulando a pausa, se ainda não foi feita). Depois da última do ciclo, volta à 1. */
    fun nextFocus(state: FocusState, settings: FocusSettings, now: Instant): FocusState {
        val next = if (state.session >= state.sessionsPerCycle) 1 else state.session + 1
        return begin(state, FocusKind.Focus, next, settings, now)
    }

    fun stop(): FocusState = FocusState()

    /** "24:59"; a partir de uma hora, "1:05:00". */
    fun format(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%02d:%02d".format(m, sec)
    }
}
