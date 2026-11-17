package com.organizador.agenda.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/*
 * Regra para não perder dados ao atualizar o app por cima:
 * mudou o schema (entidade, coluna, índice) -> version + 1, uma AutoMigration (ou Migration
 * manual, quando o Room não consegue inferir) e um teste em MigrationTest (androidTest), com o
 * JSON novo em app/schemas. NUNCA usar fallbackToDestructiveMigration: ele apaga o banco.
 * Ajustes e estado novos sem relação com tarefas vão para o DataStore, sem mexer no schema.
 */
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
