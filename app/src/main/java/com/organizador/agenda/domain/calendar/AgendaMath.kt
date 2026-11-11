package com.organizador.agenda.domain.calendar

import com.organizador.agenda.domain.model.CalendarEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/** Um evento com horário recortado para um dia: minutos desde a meia-noite desse dia. */
data class TimedSlot(val event: CalendarEvent, val startMin: Int, val endMin: Int)

/** Posição horizontal de um bloco: coluna [column] de [columns] colunas lado a lado. */
data class Placement(val column: Int, val columns: Int)

data class DayEvents(val allDay: List<CalendarEvent>, val timed: List<TimedSlot>)

/** Contas da Agenda: grade de semana e mês, eventos por dia, colunas de sobreposição, encaixe. Puro. */
object AgendaMath {
    const val MINUTES_PER_DAY = 24 * 60
    const val SNAP_MINUTES = 15

    /** Altura mínima de um bloco, em minutos, para o cálculo de sobreposição (um bloco curto ainda ocupa espaço). */
    const val MIN_BLOCK_MINUTES = 30

    /** Os 7 dias da semana que contém [date], começando em [weekStart]. */
    fun weekOf(date: LocalDate, weekStart: DayOfWeek): List<LocalDate> {
        val start = date.with(TemporalAdjusters.previousOrSame(weekStart))
        return List(7) { start.plusDays(it.toLong()) }
    }

    /** Semanas completas que cobrem o mês (inclui dias dos meses vizinhos nas pontas). */
    fun monthGrid(month: YearMonth, weekStart: DayOfWeek): List<List<LocalDate>> {
        val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(weekStart))
        val last = month.atEndOfMonth()
        return generateSequence(first) { it.plusWeeks(1) }.takeWhile { it <= last }.map { weekOf(it, weekStart) }.toList()
    }

    /** Número da semana (ISO) do dia. */
    fun weekNumber(date: LocalDate): Int = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    /** O evento toca o dia [date]? Dia inteiro é comparado em UTC (como o provider grava). */
    fun occursOn(event: CalendarEvent, date: LocalDate, zone: ZoneId): Boolean {
        val z = if (event.allDay) ZoneOffset.UTC else zone
        val dayStart = date.atStartOfDay(z).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(z).toInstant()
        // Evento de duração zero conta no instante de início.
        return event.start < dayEnd && (event.end > dayStart || event.start == dayStart && event.end == event.start)
    }

    /** Separa os eventos do dia em "dia inteiro" e com horário (recortados nos limites do dia). */
    fun forDay(events: List<CalendarEvent>, date: LocalDate, zone: ZoneId): DayEvents {
        val today = events.filter { occursOn(it, date, zone) }
        val dayStart = date.atStartOfDay(zone).toInstant()
        val timed = today.filter { !it.allDay }.map { e ->
            val start = ((e.start.toEpochMilli() - dayStart.toEpochMilli()) / 60_000).toInt().coerceIn(0, MINUTES_PER_DAY)
            val end = ((e.end.toEpochMilli() - dayStart.toEpochMilli()) / 60_000).toInt().coerceIn(start, MINUTES_PER_DAY)
            TimedSlot(e, start, end)
        }.sortedWith(compareBy({ it.startMin }, { -it.endMin }, { it.event.instanceId }))
        return DayEvents(today.filter { it.allDay }.sortedBy { it.title }, timed)
    }

    /**
     * Distribui eventos sobrepostos em colunas. Eventos que se encadeiam por sobreposição formam
     * um grupo; todos do grupo têm a mesma largura (1/colunas do grupo). Chave: instanceId.
     */
    fun layout(slots: List<TimedSlot>): Map<Long, Placement> {
        fun end(s: TimedSlot) = maxOf(s.endMin, s.startMin + MIN_BLOCK_MINUTES)
        val sorted = slots.sortedWith(compareBy({ it.startMin }, { -end(it) }, { it.event.instanceId }))
        val result = mutableMapOf<Long, Placement>()
        var cluster = mutableListOf<Pair<TimedSlot, Int>>()
        var columnEnds = mutableListOf<Int>()
        var clusterEnd = -1
        fun flush() {
            cluster.forEach { (slot, col) -> result[slot.event.instanceId] = Placement(col, columnEnds.size) }
            cluster = mutableListOf()
            columnEnds = mutableListOf()
        }
        for (slot in sorted) {
            if (cluster.isNotEmpty() && slot.startMin >= clusterEnd) flush()
            var col = columnEnds.indexOfFirst { it <= slot.startMin }
            if (col == -1) {
                col = columnEnds.size
                columnEnds.add(end(slot))
            } else {
                columnEnds[col] = end(slot)
            }
            cluster.add(slot to col)
            clusterEnd = maxOf(clusterEnd, end(slot))
        }
        flush()
        return result
    }

    /** Arredonda para o múltiplo de [step] minutos mais próximo. */
    fun snap(minutes: Int, step: Int = SNAP_MINUTES): Int = Math.floorDiv(minutes + step / 2, step) * step

    /**
     * Novo início (minutos do dia) ao arrastar um bloco [deltaMinutes]: encaixa em 15 min e mantém
     * o evento inteiro dentro do dia.
     */
    fun dragTarget(startMin: Int, durationMin: Int, deltaMinutes: Int): Int =
        snap(startMin + deltaMinutes).coerceIn(0, (MINUTES_PER_DAY - durationMin).coerceAtLeast(0))
}
