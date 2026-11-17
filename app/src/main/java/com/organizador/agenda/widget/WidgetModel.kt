package com.organizador.agenda.widget

import com.organizador.agenda.domain.calendar.NowNext
import com.organizador.agenda.domain.model.AppSettings
import com.organizador.agenda.domain.model.CalendarEvent
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.task.TaskLists
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.tasks.TaskUiMapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** [future] = tarefa de "Próximos dias": a data/hora (meta) sai em text2, não em volt. */
data class WidgetTask(val id: Long, val title: String, val meta: String?, val overdue: Boolean, val future: Boolean = false)

/** Compromisso em destaque, com horários absolutos: o widget não se redesenha a cada minuto. */
data class WidgetEvent(val live: Boolean, val label: String, val title: String)

data class WidgetState(
    val eyebrow: String,
    val done: Int,
    val total: Int,
    val event: WidgetEvent?,
    /** Tarefas abertas de hoje (atrasadas primeiro), na ordem do Hoje. */
    val tasks: List<WidgetTask>,
    /** Tarefas abertas de amanhã até o limite dos Ajustes, por data (meta com o dia). */
    val upcoming: List<WidgetTask> = emptyList(),
    /** Próximo momento em que o conteúdo muda sozinho (início/fim de compromisso, meia-noite). */
    val nextRefresh: Instant,
)

/**
 * O que cabe na altura do widget: as tarefas de hoje (primeiro), as dos próximos dias (com o
 * rótulo "Próximos dias") e quantas ficaram de fora ("+N mais").
 */
data class WidgetLayout(
    val today: List<WidgetTask>,
    val upcoming: List<WidgetTask>,
    val upcomingLabel: Boolean,
    val more: Int,
)

/** Monta o que o widget mostra. Puro (testado em JVM). */
object WidgetModel {
    /**
     * Distribui as linhas em [heightDp]. Hoje vem primeiro; os próximos dias só entram com o rótulo
     * ([labelDp]) e pelo menos uma linha ([rowDp]). Se algo não couber, uma linha fica para o "+N mais".
     */
    fun layout(state: WidgetState, heightDp: Int, rowDp: Int, labelDp: Int): WidgetLayout {
        val today = state.tasks
        val upcoming = state.upcoming
        val total = today.size + upcoming.size
        val all = today.size * rowDp + if (upcoming.isEmpty()) 0 else labelDp + upcoming.size * rowDp
        if (all <= heightDp) return WidgetLayout(today, upcoming, upcoming.isNotEmpty(), 0)

        var room = (heightDp - rowDp).coerceAtLeast(0)
        val shownToday = today.take(room / rowDp)
        room -= shownToday.size * rowDp
        val shownUpcoming =
            if (shownToday.size == today.size && upcoming.isNotEmpty() && room >= labelDp + rowDp) upcoming.take((room - labelDp) / rowDp)
            else emptyList()
        return WidgetLayout(shownToday, shownUpcoming, shownUpcoming.isNotEmpty(), total - shownToday.size - shownUpcoming.size)
    }

    fun build(
        tasks: List<Task>,
        todayEvents: List<CalendarEvent>,
        now: LocalDateTime,
        zone: ZoneId,
        upcomingDays: Int = AppSettings.DEFAULT_UPCOMING_DAYS,
    ): WidgetState {
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
            upcoming = TaskLists.upcoming(tasks, now, upcomingDays).flatMap { it.second }.map { t ->
                WidgetTask(t.id, t.title, TaskUiMapper.dueChip(t, now)?.text, overdue = false, future = true)
            },
            nextRefresh = (boundaries + midnight).min(),
        )
    }
}
