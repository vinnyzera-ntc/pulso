package com.organizador.agenda.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.task.TaskFilter
import com.organizador.agenda.domain.task.TaskLists
import com.organizador.agenda.domain.task.TaskSection
import com.organizador.agenda.domain.time.NowProvider
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.common.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class TaskSectionUi(val section: TaskSection, val rows: List<TaskRowUi>)

data class TasksUiState(
    val loaded: Boolean = false,
    val filter: TaskFilter = TaskFilter.All,
    val query: String = "",
    val searching: Boolean = false,
    val overdueCount: Int = 0,
    val tags: List<Tag> = emptyList(),
    val sections: List<TaskSectionUi> = emptyList(),
    val completed: List<TaskRowUi> = emptyList(),
    val completedExpanded: Boolean = false,
    /** Não existe nenhuma tarefa no app (diferente de "o filtro não achou nada"). */
    val noTasksAtAll: Boolean = false,
) {
    val isEmpty: Boolean get() = sections.isEmpty() && completed.isEmpty()
}

private data class ViewOptions(
    val filter: TaskFilter = TaskFilter.All,
    val query: String = "",
    val searching: Boolean = false,
    val completedExpanded: Boolean = false,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val tasks: TaskRepository,
    nowProvider: NowProvider,
    private val messenger: Messenger,
) : ViewModel() {

    private val options = MutableStateFlow(ViewOptions())

    val state: StateFlow<TasksUiState> =
        combine(tasks.observeTasks(), tasks.observeTags(), nowProvider.minuteTicks, options) { all, tags, now, o ->
            buildState(all, tags, now, o.filter, o.query, o.searching, o.completedExpanded)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun setFilter(filter: TaskFilter) = options.update { it.copy(filter = filter) }
    fun setQuery(query: String) = options.update { it.copy(query = query) }
    /** A busca procura em todas as tarefas: ao abrir, o filtro volta para "Todas". */
    fun setSearching(on: Boolean) = options.update {
        it.copy(searching = on, query = if (on) it.query else "", filter = if (on) TaskFilter.All else it.filter)
    }
    fun toggleCompleted() = options.update { it.copy(completedExpanded = !it.completedExpanded) }

    fun setDone(id: Long, done: Boolean) = viewModelScope.launch {
        tasks.setCompleted(id, done)
        // Ao concluir, a tarefa some para "Concluídas" (recolhidas): oferece desfazer.
        if (done) {
            messenger.show(UiMessage(R.string.msg_task_completed, R.string.action_undo) { tasks.setCompleted(id, false) })
        }
    }

    companion object {
        /** Montagem do estado, pura (testada em JVM). */
        fun buildState(
            all: List<Task>,
            tags: List<Tag>,
            now: LocalDateTime,
            filter: TaskFilter,
            query: String = "",
            searching: Boolean = false,
            completedExpanded: Boolean = false,
        ): TasksUiState {
            // Um filtro por etiqueta que deixou de existir volta para "Todas".
            val effective = if (filter is TaskFilter.ByTag && tags.none { it.id == filter.tagId }) TaskFilter.All else filter
            val q = query.trim()
            val searched = if (q.isEmpty()) all else all.filter { t ->
                t.title.contains(q, ignoreCase = true) || t.notes.contains(q, ignoreCase = true) ||
                    t.tags.any { it.name.contains(q.removePrefix("#"), ignoreCase = true) }
            }
            val groups = TaskLists.group(searched, effective, now)
            // Só as etiquetas em uso por tarefas abertas viram filtro.
            val usedTagIds = all.filter { !it.isDone }.flatMap { it.tags }.map { it.id }.toSet()
            return TasksUiState(
                loaded = true,
                filter = effective,
                query = query,
                searching = searching,
                overdueCount = TaskLists.overdueCount(all, now),
                tags = tags.filter { it.id in usedTagIds || (effective as? TaskFilter.ByTag)?.tagId == it.id },
                sections = groups.sections.map { (s, list) -> TaskSectionUi(s, list.map { TaskUiMapper.row(it, now) }) },
                completed = groups.completed.map { TaskUiMapper.row(it, now) },
                completedExpanded = completedExpanded,
                noTasksAtAll = all.isEmpty(),
            )
        }
    }
}
