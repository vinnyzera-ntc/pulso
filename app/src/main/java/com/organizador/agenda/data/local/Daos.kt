package com.organizador.agenda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY id")
    fun observeAll(): Flow<List<TaskWithDetails>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observe(id: Long): Flow<TaskWithDetails?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskWithDetails?

    @Insert
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET completed_at = :completedAt, updated_at = :now WHERE id = :id")
    suspend fun setCompletedAt(id: Long, completedAt: Instant?, now: Instant)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    // Subtarefas
    @Insert
    suspend fun insertSubtasks(items: List<SubtaskEntity>): List<Long>

    @Query("DELETE FROM subtasks WHERE task_id = :taskId")
    suspend fun deleteSubtasksOf(taskId: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM subtasks WHERE task_id = :taskId")
    suspend fun nextSubtaskPosition(taskId: Long): Int

    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    suspend fun setSubtaskDone(id: Long, done: Boolean)

    @Query("UPDATE subtasks SET title = :title WHERE id = :id")
    suspend fun renameSubtask(id: Long, title: String)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubtask(id: Long)

    // Etiquetas
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name")
    suspend fun tagByName(name: String): TagEntity?

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTags(refs: List<TaskTagCrossRef>)

    /** Etiquetas que não estão em nenhuma tarefa deixam de existir (e de ser sugeridas). */
    @Query("DELETE FROM tags WHERE id NOT IN (SELECT tag_id FROM task_tags)")
    suspend fun deleteOrphanTags()

    @Query("DELETE FROM task_tags WHERE task_id = :taskId")
    suspend fun deleteTagsOf(taskId: Long)

    // Lembretes
    @Insert
    suspend fun insertReminders(items: List<TaskReminderEntity>)

    @Query("DELETE FROM task_reminders WHERE task_id = :taskId")
    suspend fun deleteRemindersOf(taskId: Long)

    @Query("UPDATE task_reminders SET fired_at = :at WHERE id IN (:ids)")
    suspend fun markRemindersFired(ids: List<Long>, at: Instant)

    @Query("UPDATE task_reminders SET snoozed_until = :until WHERE id = :id")
    suspend fun snoozeReminder(id: Long, until: LocalDateTime)

    /** Ao mudar o vencimento, um "Adiar" antigo deixa de valer: os lembretes voltam ao cálculo normal. */
    @Query("UPDATE task_reminders SET snoozed_until = NULL WHERE task_id = :taskId")
    suspend fun clearSnoozes(taskId: Long)

    /**
     * Grava a tarefa e substitui todos os filhos de uma vez (atômico).
     * Etiquetas são resolvidas por nome, criando as que não existem.
     */
    @Transaction
    suspend fun saveWithChildren(
        task: TaskEntity,
        subtasks: List<SubtaskEntity>,
        tagNames: List<String>,
        reminders: List<TaskReminderEntity>,
        /** true para reinserir uma tarefa excluída mantendo o id ("Desfazer"). */
        reinsert: Boolean = false,
    ): Long {
        val id = if (task.id == 0L || reinsert) insertTask(task) else task.id.also { updateTask(task) }
        deleteSubtasksOf(id)
        insertSubtasks(subtasks.mapIndexed { i, s -> s.copy(id = 0, taskId = id, position = i) })
        replaceTags(id, tagNames)
        replaceReminders(id, reminders)
        return id
    }

    /** Troca as etiquetas da tarefa, criando as que ainda não existem. */
    @Transaction
    suspend fun replaceTags(taskId: Long, tagNames: List<String>) {
        deleteTagsOf(taskId)
        val tagIds = tagNames.distinct().map { name ->
            tagByName(name)?.id ?: insertTag(TagEntity(name = name)).takeIf { it > 0 } ?: tagByName(name)!!.id
        }
        insertTaskTags(tagIds.map { TaskTagCrossRef(taskId, it) })
        deleteOrphanTags()
    }

    @Transaction
    suspend fun replaceReminders(taskId: Long, reminders: List<TaskReminderEntity>) {
        deleteRemindersOf(taskId)
        insertReminders(reminders.map { it.copy(id = 0, taskId = taskId) })
    }

    /** Só os campos da própria tarefa; subtarefas, etiquetas e lembretes ficam como estão. */
    @Query(
        "UPDATE tasks SET title = :title, notes = :notes, priority = :priority, due_date = :dueDate, " +
            "due_time = :dueTime, updated_at = :now WHERE id = :id",
    )
    suspend fun updateFields(id: Long, title: String, notes: String, priority: Int, dueDate: LocalDate?, dueTime: LocalTime?, now: Instant)
}

@Dao
interface FocusDao {
    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Query("UPDATE focus_sessions SET ended_at = :endedAt, completed = :completed WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Instant, completed: Boolean)

    @Query("SELECT * FROM focus_sessions WHERE task_id = :taskId ORDER BY started_at")
    fun observeForTask(taskId: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE started_at >= :from AND started_at < :to ORDER BY started_at")
    fun observeBetween(from: Instant, to: Instant): Flow<List<FocusSessionEntity>>
}
