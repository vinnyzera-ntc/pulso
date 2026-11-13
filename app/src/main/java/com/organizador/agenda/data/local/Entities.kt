package com.organizador.agenda.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "tasks", indices = [Index("due_date"), Index("completed_at")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    /** 1 (P1) a 4 (P4). */
    val priority: Int = 4,
    @ColumnInfo(name = "due_date") val dueDate: LocalDate? = null,
    @ColumnInfo(name = "due_time") val dueTime: LocalTime? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Instant? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    @ColumnInfo(name = "updated_at") val updatedAt: Instant,
    /** Recurrence.encode() ("W|1|1,3"); null = não se repete. Coluna nova na versão 2 do banco. */
    @ColumnInfo(name = "recurrence") val recurrence: String? = null,
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [ForeignKey(TaskEntity::class, ["id"], ["task_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("task_id")],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    val title: String,
    val done: Boolean = false,
    val position: Int = 0,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Guardado sem "#", em minúsculas, para não duplicar "#Pessoal" e "#pessoal". */
    val name: String,
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["task_id", "tag_id"],
    foreignKeys = [
        ForeignKey(TaskEntity::class, ["id"], ["task_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TagEntity::class, ["id"], ["tag_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("tag_id")],
)
data class TaskTagCrossRef(
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

@Entity(
    tableName = "task_reminders",
    foreignKeys = [ForeignKey(TaskEntity::class, ["id"], ["task_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("task_id")],
)
data class TaskReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    @ColumnInfo(name = "offset_minutes") val offsetMinutes: Int = 0,
    @ColumnInfo(name = "snoozed_until") val snoozedUntil: LocalDateTime? = null,
    @ColumnInfo(name = "fired_at") val firedAt: Instant? = null,
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [ForeignKey(TaskEntity::class, ["id"], ["task_id"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("task_id"), Index("started_at")],
)
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long?,
    /** Nome de FocusKind. */
    val kind: String,
    @ColumnInfo(name = "planned_minutes") val plannedMinutes: Int,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "ended_at") val endedAt: Instant? = null,
    val completed: Boolean = false,
)

/** Tarefa com tudo o que pende dela, numa leitura só. */
data class TaskWithDetails(
    @Embedded val task: TaskEntity,
    @Relation(parentColumn = "id", entityColumn = "task_id")
    val subtasks: List<SubtaskEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TaskTagCrossRef::class, parentColumn = "task_id", entityColumn = "tag_id"),
    )
    val tags: List<TagEntity>,
    @Relation(parentColumn = "id", entityColumn = "task_id")
    val reminders: List<TaskReminderEntity>,
)
