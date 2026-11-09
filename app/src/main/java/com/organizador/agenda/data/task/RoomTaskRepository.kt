package com.organizador.agenda.data.task

import com.organizador.agenda.data.local.SubtaskEntity
import com.organizador.agenda.data.local.TaskDao
import com.organizador.agenda.data.local.TaskReminderEntity
import com.organizador.agenda.data.local.normalizeTagName
import com.organizador.agenda.data.local.toDomain
import com.organizador.agenda.data.local.toEntity
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTaskRepository @Inject constructor(
    private val dao: TaskDao,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeTask(id: Long): Flow<Task?> = dao.observe(id).map { it?.toDomain() }

    override suspend fun getTask(id: Long): Task? = dao.get(id)?.toDomain()

    override suspend fun save(task: Task): Long = write(task, reinsert = false)

    override suspend fun restore(task: Task) {
        write(task, reinsert = true)
    }

    private suspend fun write(task: Task, reinsert: Boolean): Long {
        require(task.title.isNotBlank()) { "Tarefa sem título" }
        return dao.saveWithChildren(
            task = task.toEntity(Instant.now(clock)),
            subtasks = task.subtasks.filter { it.title.isNotBlank() }
                .map { SubtaskEntity(id = 0, taskId = task.id, title = it.title.trim(), done = it.done) },
            tagNames = task.tags.map { normalizeTagName(it.name) }.filter { it.isNotEmpty() },
            reminders = task.reminders.distinctBy { it.offsetMinutes to it.snoozedUntil }.map {
                TaskReminderEntity(taskId = task.id, offsetMinutes = it.offsetMinutes, snoozedUntil = it.snoozedUntil, firedAt = it.firedAt)
            },
            reinsert = reinsert,
        )
    }

    override suspend fun setCompleted(id: Long, completed: Boolean) {
        val now = Instant.now(clock)
        dao.setCompletedAt(id, if (completed) now else null, now)
    }

    override suspend fun delete(id: Long): Task? {
        val snapshot = dao.get(id)?.toDomain() ?: return null
        dao.deleteTask(id)
        return snapshot
    }

    override suspend fun addSubtask(taskId: Long, title: String): Long {
        val position = dao.nextSubtaskPosition(taskId)
        return dao.insertSubtasks(listOf(SubtaskEntity(taskId = taskId, title = title.trim(), position = position))).first()
    }

    override suspend fun setSubtaskDone(subtaskId: Long, done: Boolean) = dao.setSubtaskDone(subtaskId, done)

    override suspend fun renameSubtask(subtaskId: Long, title: String) = dao.renameSubtask(subtaskId, title.trim())

    override suspend fun deleteSubtask(subtaskId: Long) = dao.deleteSubtask(subtaskId)

    override fun observeTags(): Flow<List<Tag>> = dao.observeTags().map { list -> list.map { Tag(it.id, it.name) } }
}
