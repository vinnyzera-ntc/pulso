package com.organizador.agenda.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TaskEntity::class,
        SubtaskEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        TaskReminderEntity::class,
        FocusSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
    // 1 -> 2: coluna tasks.recurrence (tarefas que se repetem). Sem perda de dados.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class PulsoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusDao(): FocusDao

    companion object {
        const val NAME = "pulso.db"
    }
}
