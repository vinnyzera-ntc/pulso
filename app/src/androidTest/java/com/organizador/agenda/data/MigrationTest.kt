package com.organizador.agenda.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.organizador.agenda.data.local.PulsoDatabase
import com.organizador.agenda.data.local.toDomain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** O banco do aparelho do usuário está na versão 1: a 2 (repetição) não pode perder nada. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migracao-teste.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PulsoDatabase::class.java)

    @Test
    fun versao1Para2_mantemTarefasESubtarefasELembretes() = runTest {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO tasks (id, title, notes, priority, due_date, due_time, completed_at, created_at, updated_at) " +
                    "VALUES (7, 'Pagar aluguel', 'pix', 2, 20762, 32400, NULL, 1000, 1000)",
            )
            execSQL("INSERT INTO subtasks (id, task_id, title, done, position) VALUES (1, 7, 'Conferir valor', 1, 0)")
            execSQL("INSERT INTO task_reminders (id, task_id, offset_minutes, snoozed_until, fired_at) VALUES (1, 7, 15, NULL, NULL)")
            close()
        }
        helper.runMigrationsAndValidate(dbName, 2, true)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.databaseBuilder(context, PulsoDatabase::class.java, dbName).build()
        try {
            val task = db.taskDao().get(7)!!.toDomain()
            assertEquals("Pagar aluguel", task.title)
            assertEquals("pix", task.notes)
            assertEquals(LocalDate.of(2026, 11, 5), task.dueDate)
            assertEquals(listOf("Conferir valor"), task.subtasks.map { it.title })
            assertEquals(listOf(15), task.reminders.map { it.offsetMinutes })
            assertNull("tarefas antigas não se repetem", task.recurrence)
        } finally {
            db.close()
            context.deleteDatabase(dbName)
        }
    }
}
