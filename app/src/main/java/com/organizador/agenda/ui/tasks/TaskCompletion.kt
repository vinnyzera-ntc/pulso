package com.organizador.agenda.ui.tasks

import com.organizador.agenda.R
import com.organizador.agenda.domain.repository.TaskRepository
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.Messenger
import com.organizador.agenda.ui.common.UiMessage

/**
 * Conclui ou reabre uma tarefa. Se ela se repete, vai para a próxima data e o aviso diz qual
 * ("Próxima: Sex, 13 nov"), com Desfazer que devolve a tarefa como estava. Uma tarefa comum
 * concluída só ganha aviso quando [confirmDone] (na lista de Tarefas ela some para "Concluídas").
 */
suspend fun setTaskDone(tasks: TaskRepository, messenger: Messenger, id: Long, done: Boolean, confirmDone: Boolean) {
    val before = if (done) tasks.getTask(id) else null
    val next = tasks.setCompleted(id, done)
    when {
        next != null && before != null ->
            messenger.show(UiMessage(R.string.msg_task_next, R.string.action_undo, args = listOf(PtBr.shortDate(next))) { tasks.save(before) })
        done && confirmDone ->
            messenger.show(UiMessage(R.string.msg_task_completed, R.string.action_undo) { tasks.setCompleted(id, false) })
    }
}
