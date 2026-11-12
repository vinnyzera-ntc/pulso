package com.organizador.agenda.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.organizador.agenda.R
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.focus.FocusSelection
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.common.UiMessage
import com.organizador.agenda.ui.navigation.TaskDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class TaskDetailUiState(
    val loaded: Boolean = false,
    /** null depois de carregado = a tarefa não existe mais. */
    val task: Task? = null,
    val allTags: List<Tag> = emptyList(),
    val now: LocalDateTime = LocalDateTime.MIN,
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tasks: TaskRepository,
    nowProvider: NowProvider,
    private val messenger: Messenger,
    private val focusSelection: FocusSelection,
    /** Edições de texto são gravadas ao sair da tela, quando o escopo do ViewModel já foi cancelado. */
    @param:ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val id: Long = savedStateHandle.toRoute<TaskDetailRoute>().id

    val state: StateFlow<TaskDetailUiState> =
        combine(tasks.observeTask(id), tasks.observeTags(), nowProvider.minuteTicks) { task, tags, now ->
            TaskDetailUiState(loaded = true, task = task, allTags = tags, now = now)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState())

    private val closeChannel = Channel<Unit>(Channel.CONFLATED)

    /** Pede para a tela fechar (depois de excluir). */
    val close = closeChannel.receiveAsFlow()

    private fun edit(transform: (Task) -> Task) = appScope.launch {
        val current = tasks.getTask(id) ?: return@launch
        val updated = transform(current)
        if (updated != current && updated.title.isNotBlank()) tasks.updateFields(updated)
    }

    fun rename(title: String) = edit { it.copy(title = title.trim()) }
    fun setNotes(notes: String) = edit { it.copy(notes = notes.trim()) }
    fun setPriority(priority: Priority) = edit { it.copy(priority = priority) }
    fun setWhen(date: LocalDate?, time: LocalTime?) = edit { it.copy(dueDate = date, dueTime = if (date == null) null else time) }

    fun toggleTag(name: String) = viewModelScope.launch {
        val current = tasks.getTask(id)?.tags?.map { it.name } ?: return@launch
        tasks.setTags(id, if (name in current) current - name else current + name)
    }

    fun toggleReminder(offsetMinutes: Int) = viewModelScope.launch {
        val current = tasks.getTask(id)?.reminders?.map { it.offsetMinutes } ?: return@launch
        tasks.setReminders(id, if (offsetMinutes in current) current - offsetMinutes else current + offsetMinutes)
    }

    fun addSubtask(title: String) = viewModelScope.launch {
        if (title.isNotBlank()) tasks.addSubtask(id, title)
    }

    fun setSubtaskDone(subtaskId: Long, done: Boolean) = viewModelScope.launch { tasks.setSubtaskDone(subtaskId, done) }

    /** Renomear para vazio apaga a subtarefa. */
    fun renameSubtask(subtaskId: Long, title: String) = appScope.launch {
        if (title.isBlank()) tasks.deleteSubtask(subtaskId) else tasks.renameSubtask(subtaskId, title)
    }

    fun deleteSubtask(subtaskId: Long) = viewModelScope.launch { tasks.deleteSubtask(subtaskId) }

    fun setDone(done: Boolean) = viewModelScope.launch { tasks.setCompleted(id, done) }

    /** Deixa esta tarefa escolhida na aba Foco. */
    fun selectForFocus() = focusSelection.select(id)

    fun delete() = appScope.launch {
        val snapshot = tasks.delete(id) ?: return@launch
        closeChannel.trySend(Unit)
        messenger.show(UiMessage(R.string.msg_task_deleted, R.string.action_undo) { tasks.restore(snapshot) })
    }

    companion object {
        /** Opções de lembrete, em minutos antes do vencimento. */
        val REMINDER_OPTIONS = listOf(0, 10, 30, 60, 1440)
    }
}
