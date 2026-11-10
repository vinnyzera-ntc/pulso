package com.organizador.agenda.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.organizador.agenda.data.focus.RoomFocusRepository
import com.organizador.agenda.data.local.PulsoDatabase
import com.organizador.agenda.data.task.RoomTaskRepository
import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Subtask
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.model.TaskReminder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {
    private lateinit var db: PulsoDatabase
    private lateinit var tasks: RoomTaskRepository
    private lateinit var focus: RoomFocusRepository
    private val clock = Clock.fixed(Instant.parse("2026-11-09T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PulsoDatabase::class.java).allowMainThreadQueries().build()
        tasks = RoomTaskRepository(db.taskDao(), clock)
        focus = RoomFocusRepository(db.focusDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun sample() = Task(
        title = "Ligar pro contador",
        priority = Priority.P2,
        dueDate = LocalDate.of(2026, 11, 10),
        dueTime = LocalTime.of(9, 0),
        subtasks = listOf(Subtask(title = "Separar notas"), Subtask(title = "Conferir DARF", done = true)),
        tags = listOf(Tag(name = "#Pessoal"), Tag(name = "pessoal"), Tag(name = "Finanças")),
        reminders = listOf(TaskReminder(offsetMinutes = 15), TaskReminder(offsetMinutes = 15), TaskReminder(offsetMinutes = 0)),
    )

    @Test
    fun salvarELer_comFilhos_etiquetasNormalizadasEDeduplicadas() = runTest {
        val id = tasks.save(sample())
        val t = tasks.getTask(id)!!
        assertEquals("Ligar pro contador", t.title)
        assertEquals(Priority.P2, t.priority)
        assertEquals(listOf("Separar notas", "Conferir DARF"), t.subtasks.map { it.title })
        assertEquals(listOf(false, true), t.subtasks.map { it.done })
        assertEquals(listOf("finanças", "pessoal"), t.tags.map { it.name })
        assertEquals(listOf(0, 15), t.reminders.map { it.offsetMinutes })
        assertEquals(Instant.now(clock), t.createdAt)
    }

    @Test
    fun atualizar_substituiFilhosEReusaEtiquetas() = runTest {
        val id = tasks.save(sample())
        val t = tasks.getTask(id)!!
        tasks.save(t.copy(title = "Ligar pro contador (2)", subtasks = listOf(Subtask(title = "Só uma")), tags = listOf(Tag(name = "pessoal"))))
        val u = tasks.getTask(id)!!
        assertEquals("Ligar pro contador (2)", u.title)
        assertEquals(listOf("Só uma"), u.subtasks.map { it.title })
        assertEquals(listOf("pessoal"), u.tags.map { it.name })
        assertEquals(t.tags.first { it.name == "pessoal" }.id, u.tags.single().id)
        // Etiqueta que ficou sem nenhuma tarefa é removida.
        assertEquals(listOf("pessoal"), tasks.observeTags().first().map { it.name })
        tasks.delete(id)
        assertTrue(tasks.observeTags().first().isEmpty())
    }

    @Test
    fun concluirEReabrir() = runTest {
        val id = tasks.save(sample())
        tasks.setCompleted(id, true)
        assertEquals(Instant.now(clock), tasks.getTask(id)!!.completedAt)
        tasks.setCompleted(id, false)
        assertNull(tasks.getTask(id)!!.completedAt)
    }

    @Test
    fun excluirEDesfazer_mantemIdEFilhos() = runTest {
        val id = tasks.save(sample())
        val sessionId = focus.start(id, FocusKind.Focus, 25)
        val snapshot = tasks.delete(id)
        assertNotNull(snapshot)
        assertNull(tasks.getTask(id))
        // Sessão de foco sobrevive, desvinculada (SET_NULL).
        assertEquals(listOf(sessionId), focus.observeBetween(Instant.EPOCH, Instant.parse("2100-01-01T00:00:00Z")).first().map { it.id })
        assertNull(focus.observeBetween(Instant.EPOCH, Instant.parse("2100-01-01T00:00:00Z")).first().single().taskId)

        tasks.restore(snapshot!!)
        val r = tasks.getTask(id)!!
        assertEquals(snapshot.title, r.title)
        assertEquals(snapshot.subtasks.map { it.title to it.done }, r.subtasks.map { it.title to it.done })
        assertEquals(snapshot.tags.map { it.name }, r.tags.map { it.name })
        assertEquals(snapshot.createdAt, r.createdAt)
    }

    @Test
    fun subtarefas_adicionarMarcarRenomearExcluir() = runTest {
        val id = tasks.save(Task(title = "Viagem"))
        val a = tasks.addSubtask(id, " Passagem ")
        val b = tasks.addSubtask(id, "Hotel")
        tasks.setSubtaskDone(a, true)
        tasks.renameSubtask(b, "Hotel em Ouro Preto")
        var t = tasks.getTask(id)!!
        assertEquals(listOf("Passagem" to true, "Hotel em Ouro Preto" to false), t.subtasks.map { it.title to it.done })
        assertEquals(listOf(0, 1), t.subtasks.map { it.position })
        tasks.deleteSubtask(a)
        t = tasks.getTask(id)!!
        assertEquals(listOf("Hotel em Ouro Preto"), t.subtasks.map { it.title })
    }

    @Test
    fun observeTasks_emiteAoMudar() = runTest {
        assertTrue(tasks.observeTasks().first().isEmpty())
        tasks.save(Task(title = "A"))
        tasks.save(Task(title = "B"))
        assertEquals(listOf("A", "B"), tasks.observeTasks().first().map { it.title })
    }

    @Test
    fun foco_iniciarEFinalizar() = runTest {
        val id = tasks.save(Task(title = "Relatório"))
        val s = focus.start(id, FocusKind.Focus, 25)
        focus.finish(s, completed = true)
        val session = focus.observeForTask(id).first().single()
        assertTrue(session.completed)
        assertEquals(Instant.now(clock), session.endedAt)
        assertEquals(25, session.plannedMinutes)
    }

    @Test
    fun atualizarCampos_naoRecriaSubtarefas_eTrocaEtiquetasELembretes() = runTest {
        val id = tasks.save(sample())
        val before = tasks.getTask(id)!!
        tasks.updateFields(before.copy(title = "Novo título", notes = "nota", priority = Priority.P1, dueDate = null, dueTime = LocalTime.NOON))
        tasks.setTags(id, listOf("#Casa", "pessoal"))
        tasks.setReminders(id, listOf(60, 60, 0))
        val after = tasks.getTask(id)!!
        assertEquals("Novo título", after.title)
        assertEquals("nota", after.notes)
        assertEquals(Priority.P1, after.priority)
        assertNull(after.dueDate)
        assertNull("sem data não há hora", after.dueTime)
        assertEquals("ids das subtarefas preservados", before.subtasks.map { it.id }, after.subtasks.map { it.id })
        assertEquals(listOf("casa", "pessoal"), after.tags.map { it.name })
        assertEquals(listOf(0, 60), after.reminders.map { it.offsetMinutes })
    }
}
