package com.organizador.agenda.ui.focus

import android.Manifest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.domain.focus.FocusMachine
import com.organizador.agenda.domain.focus.FocusState
import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.FocusSettings
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.task.TaskLists
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.focus.FocusController
import com.organizador.agenda.focus.FocusSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

data class FocusTaskUi(val id: Long, val title: String)

data class FocusUiState(
    val loaded: Boolean = false,
    // ---------- preparação ----------
    val tasks: List<FocusTaskUi> = emptyList(),
    val selectedTaskId: Long? = null,
    val settings: FocusSettings = FocusSettings(),
    val notificationPermissionRequested: Boolean = false,
    // ---------- sessão ----------
    val active: Boolean = false,
    val phase: FocusKind = FocusKind.Focus,
    val running: Boolean = false,
    val finished: Boolean = false,
    /** "24:59" (tabular; os segundos trocam sem animação). */
    val timeText: String = "",
    val session: Int = 1,
    val sessionsPerCycle: Int = 4,
    val taskTitle: String? = null,
    /** A pausa que vem depois desta sessão de foco. */
    val nextBreak: FocusKind = FocusKind.ShortBreak,
    val nextBreakMinutes: Int = 5,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FocusViewModel @Inject constructor(
    private val controller: FocusController,
    private val selection: FocusSelection,
    private val settings: SettingsRepository,
    tasks: TaskRepository,
    private val nowProvider: NowProvider,
    private val clock: Clock,
) : ViewModel() {

    /** Um tique por segundo, só enquanto o timer corre (a tela não precisa acordar parada). */
    private val seconds: Flow<Instant> = controller.state.map { it.running }.distinctUntilChanged().flatMapLatest { running ->
        flow {
            emit(Instant.now(clock))
            while (running) {
                val now = Instant.now(clock)
                delay(1000 - now.toEpochMilli() % 1000)
                emit(Instant.now(clock))
            }
        }
    }

    val state: StateFlow<FocusUiState> =
        combine(controller.state, seconds, tasks.observeTasks(), settings.settings, selection.taskId) { focus, now, all, s, selected ->
            buildState(focus, now, all, nowProvider.now(), s.focus, selected, Manifest.permission.POST_NOTIFICATIONS in s.requestedPermissions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState())

    fun select(taskId: Long?) = selection.select(taskId)
    fun start() = controller.start(state.value.selectedTaskId)
    fun pause() = controller.pause()
    fun resume() = controller.resume()
    fun startBreak() = controller.startBreak()
    fun nextFocus() = controller.nextFocus()
    fun stop() = controller.stop()
    fun completeTask() = controller.completeTask()

    fun onNotificationPermissionRequested() = viewModelScope.launch {
        settings.markPermissionRequested(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        /** Montagem do estado, pura (testada em JVM). */
        fun buildState(
            focus: FocusState,
            now: Instant,
            allTasks: List<Task>,
            today: LocalDateTime,
            settings: FocusSettings,
            selectedTaskId: Long?,
            notificationPermissionRequested: Boolean,
        ): FocusUiState {
            val open = allTasks.filter { !it.isDone }
            // Sugestões: as tarefas abertas do dia (atrasadas primeiro).
            val todays = open.filter { it.dueDate != null && it.dueDate <= today.toLocalDate() }.sortedWith(TaskLists.openOrder)
            val selected = open.firstOrNull { it.id == selectedTaskId }
            // A escolhida só vai para o topo se não estiver entre as do dia (senão a lista pularia ao tocar).
            val options = (listOfNotNull(selected?.takeIf { it !in todays }) + todays).map { FocusTaskUi(it.id, it.title) }
            val nextBreak = FocusMachine.breakAfter(focus)
            return FocusUiState(
                loaded = true,
                tasks = options,
                selectedTaskId = selected?.id,
                settings = settings,
                notificationPermissionRequested = notificationPermissionRequested,
                active = focus.active,
                phase = focus.phase,
                running = focus.running,
                finished = focus.finished,
                timeText = FocusMachine.format(focus.remainingSeconds(now)),
                session = focus.session,
                sessionsPerCycle = focus.sessionsPerCycle,
                taskTitle = focus.taskTitle,
                nextBreak = nextBreak,
                nextBreakMinutes = FocusMachine.minutesOf(nextBreak, settings),
            )
        }
    }
}
