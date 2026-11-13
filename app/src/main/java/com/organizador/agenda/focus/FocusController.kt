package com.organizador.agenda.focus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.organizador.agenda.alarm.setWakeUpAt
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.domain.focus.FocusMachine
import com.organizador.agenda.domain.focus.FocusState
import com.organizador.agenda.domain.repository.FocusRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Dono do estado do modo foco. A tela, a notificação e o serviço só observam [state] e chamam
 * as ações daqui. O fim de cada fase é garantido de dois jeitos: uma espera em corrotina (rápida,
 * com o app acordado) e um alarme exato (acorda o aparelho com a tela apagada). Os dois caem em
 * [onPhaseDue], que só age uma vez.
 */
@Singleton
class FocusController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val sessions: FocusRepository,
    private val tasks: TaskRepository,
    private val notifier: FocusNotifier,
    private val clock: Clock,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(FocusState())
    val state: StateFlow<FocusState> = _state

    private val mutex = Mutex()
    private var waitJob: Job? = null

    /** Sessão aberta no banco para a fase em andamento. */
    private var sessionId: Long? = null

    private fun now(): Instant = Instant.now(clock)

    fun start(taskId: Long?) = change { _ ->
        val title = taskId?.let { tasks.getTask(it)?.title }
        FocusMachine.start(taskId.takeIf { title != null }, title, settings.settings.first().focus, now()).also { open(it) }
    }

    fun pause() = change { FocusMachine.pause(it, now()) }
    fun resume() = change { FocusMachine.resume(it, now()) }

    /** Depois de um foco concluído: começa a pausa (curta ou longa). */
    fun startBreak() = change { old ->
        close(old, completed = old.finished)
        FocusMachine.startBreak(old, settings.settings.first().focus, now()).also { open(it) }
    }

    /** Próxima sessão de foco (também serve para pular uma pausa). */
    fun nextFocus() = change { old ->
        close(old, completed = old.finished)
        FocusMachine.nextFocus(old, settings.settings.first().focus, now()).also { open(it) }
    }

    fun stop() = change { old ->
        close(old, completed = old.finished)
        FocusMachine.stop()
    }

    /** Conclui a tarefa em foco e encerra. */
    fun completeTask() = change { old ->
        old.taskId?.let { tasks.setCompleted(it, true) }
        close(old, completed = old.finished)
        FocusMachine.stop()
    }

    /** Chamado pela espera e pelo alarme quando o fim da fase chega. */
    fun onPhaseDue() = change { old ->
        val new = FocusMachine.elapse(old, now())
        if (new !== old) {
            close(old, completed = true)
            notifier.phaseDone(new)
        }
        new
    }

    private fun change(transform: suspend (FocusState) -> FocusState) = scope.launch {
        mutex.withLock {
            val old = _state.value
            val new = transform(old)
            if (new == old) return@withLock
            _state.value = new
            schedule(new)
            if (new.active && !old.active) FocusService.start(context)
            // O aviso de fim de fase sai da barra ao encerrar ou quando a próxima fase começa.
            if (!new.active || (old.finished && !new.finished)) notifier.cancelDone()
        }
    }

    private suspend fun open(state: FocusState) {
        sessionId = sessions.start(state.taskId, state.phase, (state.totalSeconds / 60).toInt())
    }

    private suspend fun close(state: FocusState, completed: Boolean) {
        // Uma fase já terminada teve a sessão fechada em onPhaseDue.
        val id = sessionId ?: return
        sessionId = null
        if (state.active) sessions.finish(id, completed)
    }

    /** (Re)arma a espera e o alarme para o fim da fase; cancela os dois se o timer não corre. */
    private fun schedule(state: FocusState) {
        waitJob?.cancel()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, 0, Intent(context, FocusAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.cancel(intent)
        val end = state.endsAt ?: return
        waitJob = scope.launch {
            delay(Duration.between(now(), end).toMillis().coerceAtLeast(0))
            onPhaseDue()
        }
        alarms.setWakeUpAt(end.toEpochMilli(), intent)
    }

    companion object {
        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
