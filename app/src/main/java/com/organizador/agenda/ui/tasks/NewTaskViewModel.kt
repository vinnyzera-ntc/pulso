package com.organizador.agenda.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.model.TaskReminder
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** O que a folha "Nova tarefa" coleta. */
data class NewTaskDraft(
    val title: String = "",
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val priority: Priority = Priority.P4,
    /** Minutos antes; null = sem lembrete. Só vale com data. */
    val reminderMinutes: Int? = null,
    val tags: Set<String> = emptySet(),
) {
    val isValid: Boolean get() = title.isNotBlank()

    fun toTask(): Task = Task(
        title = title.trim(),
        priority = priority,
        dueDate = date,
        dueTime = if (date == null) null else time,
        tags = tags.map { Tag(name = it) },
        reminders = if (date != null && reminderMinutes != null) listOf(TaskReminder(offsetMinutes = reminderMinutes)) else emptyList(),
    )
}

@HiltViewModel
class NewTaskViewModel @Inject constructor(
    private val tasks: TaskRepository,
    private val nowProvider: NowProvider,
    private val messenger: Messenger,
) : ViewModel() {

    val tags: StateFlow<List<Tag>> = tasks.observeTags().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun today(): LocalDate = nowProvider.now().toLocalDate()

    fun create(draft: NewTaskDraft) = viewModelScope.launch {
        if (!draft.isValid) return@launch
        val id = tasks.save(draft.toTask())
        messenger.show(UiMessage(R.string.msg_task_created, R.string.action_undo) { tasks.delete(id) })
    }

    companion object {
        val REMINDER_OPTIONS = listOf<Int?>(null, 0, 10, 60)
    }
}
