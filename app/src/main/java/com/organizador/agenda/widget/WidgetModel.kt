package com.organizador.agenda.widget

import com.organizador.agenda.domain.calendar.NowNext
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.task.TaskLists
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.tasks.TaskUiMapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

data class WidgetTask(val id: Long, val title: String, val meta: String?, val overdue: Boolean)

/** Compromisso em destaque, com horários absolutos: o widget não se redesenha a cada minuto. */
data class WidgetEvent(val live: Boolean, val label: String, val title: String)

data class WidgetState(
    val eyebrow: String,
    val done: Int,
    val total: Int,
    val event: WidgetEvent?,
    /** Tarefas abertas de hoje (atrasadas primeiro), na ordem do Hoje. */
    val tasks: List<WidgetTask>,
    /** Próximo momento em que o conteúdo muda sozinho (início/fim de compromisso, meia-noite). */
    val nextRefresh: Instant,
)

/** Monta o que o widget mostra. Puro (testado em JVM). */
object WidgetModel {
    fun build(tasks: List<Task>, todayEvents: List<CalendarEvent>, now: LocalDateTime, zone: ZoneId): WidgetState {
        val today = TaskLists.today(tasks, now, zone)
        val progress = TaskLists.progress(today)
        val instant = now.atZone(zone).toInstant()
        fun time(i: Instant) = PtBr.time(LocalDateTime.ofInstant(i, zone).toLocalTime())
        val event = when (val pick = NowNext.pick(todayEvents, instant)) {
            is NowNext.Current -> WidgetEvent(live = true, label = "Agora · até ${time(pick.event.end)}", title = pick.event.title)
            is NowNext.Next -> WidgetEvent(live = false, label = "Próximo · ${time(pick.event.start)}", title = pick.event.title)
            NowNext.None -> null
        }
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
        val boundaries = todayEvents.filter { !it.allDay }.flatMap { listOf(it.start, it.end) }.filter { it > instant }
        return WidgetState(
            eyebrow = PtBr.eyebrowDate(now.toLocalDate()),
            done = progress.done,
            total = progress.total,
            event = event,
            tasks = today.filter { !it.isDone }.map { t ->
                WidgetTask(t.id, t.title, TaskUiMapper.dueChip(t, now)?.text, TaskLists.isOverdue(t, now))
            },
            nextRefresh = (boundaries + midnight).min(),
        )
    }
}
