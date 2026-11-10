package com.organizador.agenda.debug

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Subtask
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.model.TaskReminder
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dados de exemplo para verificação no aparelho. Só é chamado em build debug, pelo extra
 * `-e seed <modo>` da MainActivity:
 *  - `demo`: tarefas de exemplo + agenda local "Pulso Teste" com um compromisso acontecendo agora;
 *  - `demo-proximo`: igual, mas o primeiro compromisso começa em 25 min;
 *  - `limpar`: remove exatamente o que o seeder criou (ids guardados), nada mais.
 *
 * A agenda "Pulso Teste" é LOCAL (não sincroniza com o Google).
 */
@Singleton
class DebugSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tasks: TaskRepository,
    private val calendar: CalendarRepository,
    private val clock: Clock,
) {
    private val prefs get() = context.getSharedPreferences("debug_seed", Context.MODE_PRIVATE)

    suspend fun run(mode: String) = withContext(Dispatchers.IO) {
        clear()
        if (mode == "demo" || mode == "demo-proximo") {
            seedTasks()
            if (calendar.hasPermission()) seedCalendar(liveNow = mode == "demo")
        }
        calendar.refresh()
    }

    private suspend fun clear() {
        prefs.getStringSet(KEY_TASKS, emptySet()).orEmpty().forEach { tasks.delete(it.toLong()) }
        prefs.edit().remove(KEY_TASKS).apply()
        if (calendar.hasPermission()) {
            context.contentResolver.delete(
                asSyncAdapter(Calendars.CONTENT_URI),
                "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ?",
                arrayOf(ACCOUNT, CalendarContract.ACCOUNT_TYPE_LOCAL),
            )
        }
    }

    private suspend fun seedTasks() {
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        // Horários "redondos" à frente, sem passar da meia-noite.
        fun ahead(hours: Long): LocalTime {
            val t = now.plusHours(hours).truncatedTo(ChronoUnit.HOURS)
            return if (t.toLocalDate() == today) t.toLocalTime() else LocalTime.of(23, 30)
        }
        fun doneAgo(minutes: Long): Instant = Instant.now(clock).minus(minutes, ChronoUnit.MINUTES)
        fun tags(vararg n: String) = n.map { Tag(name = it) }

        val items = listOf(
            Task(title = "Revisar proposta do cliente", priority = Priority.P1, dueDate = today.minusDays(1), tags = tags("trabalho")),
            Task(
                title = "Enviar relatório mensal", priority = Priority.P2, dueDate = today, dueTime = ahead(2),
                tags = tags("trabalho"), reminders = listOf(TaskReminder(offsetMinutes = 10)),
            ),
            Task(title = "Ligar pro contador", priority = Priority.P3, dueDate = today, dueTime = ahead(4), tags = tags("pessoal")),
            Task(
                title = "Comprar presente da Ana", dueDate = today, tags = tags("pessoal"),
                subtasks = listOf(Subtask(title = "Escolher o livro", done = true), Subtask(title = "Passar na livraria"), Subtask(title = "Embrulhar")),
                notes = "Ela comentou do novo romance da Carla Madeira.",
            ),
            Task(title = "Agendar revisão do carro", dueDate = today, completedAt = doneAgo(190)),
            Task(title = "Pagar boleto do condomínio", priority = Priority.P2, dueDate = today, completedAt = doneAgo(120)),
            Task(title = "Responder e-mails pendentes", dueDate = today, completedAt = doneAgo(45), tags = tags("trabalho")),
            Task(
                title = "Preparar apresentação de sexta", priority = Priority.P1, dueDate = today.plusDays(1), dueTime = LocalTime.of(10, 0),
                tags = tags("trabalho"),
                subtasks = listOf(Subtask(title = "Roteiro", done = true), Subtask(title = "Slides"), Subtask(title = "Ensaiar")),
                reminders = listOf(TaskReminder(offsetMinutes = 60)),
            ),
            Task(title = "Renovar seguro do carro", priority = Priority.P3, dueDate = today.plusDays(3)),
            Task(title = "Marcar consulta no dentista", dueDate = today.plusDays(12), tags = tags("pessoal")),
            Task(title = "Organizar fotos da viagem"),
            Task(title = "Ler capítulo 4 do livro", priority = Priority.P3, tags = tags("estudos")),
            Task(title = "Devolver o livro da biblioteca", dueDate = today.minusDays(2), completedAt = doneAgo(60 * 26)),
        )
        val ids = items.map { tasks.save(it).toString() }.toSet()
        prefs.edit().putStringSet(KEY_TASKS, ids).apply()
    }

    private fun seedCalendar(liveNow: Boolean) {
        val resolver = context.contentResolver
        val calendarUri = resolver.insert(
            asSyncAdapter(Calendars.CONTENT_URI),
            ContentValues().apply {
                put(Calendars.ACCOUNT_NAME, ACCOUNT)
                put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                put(Calendars.NAME, ACCOUNT)
                put(Calendars.CALENDAR_DISPLAY_NAME, ACCOUNT)
                put(Calendars.CALENDAR_COLOR, 0xFF2F95F5.toInt())
                put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
                put(Calendars.OWNER_ACCOUNT, ACCOUNT)
                put(Calendars.VISIBLE, 1)
                put(Calendars.SYNC_EVENTS, 1)
                put(Calendars.CALENDAR_TIME_ZONE, clock.zone.id)
            },
        ) ?: return
        val calendarId = ContentUris.parseId(calendarUri)
        val now = Instant.now(clock)
        fun event(title: String, startInMin: Long, minutes: Long, location: String?) {
            resolver.insert(
                Events.CONTENT_URI,
                ContentValues().apply {
                    put(Events.CALENDAR_ID, calendarId)
                    put(Events.TITLE, title)
                    put(Events.DTSTART, now.plus(startInMin, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES).toEpochMilli())
                    put(Events.DTEND, now.plus(startInMin + minutes, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES).toEpochMilli())
                    put(Events.EVENT_TIMEZONE, clock.zone.id)
                    location?.let { put(Events.EVENT_LOCATION, it) }
                },
            )
        }
        if (liveNow) event("Alinhamento semanal do projeto", -25, 60, "Sala 3 · Prédio B")
        event("Conversa com a Ana sobre o orçamento", if (liveNow) 90 else 25, 45, "Google Meet")
    }

    private fun asSyncAdapter(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    private companion object {
        const val ACCOUNT = "Pulso Teste"
        const val KEY_TASKS = "task_ids"
    }
}
