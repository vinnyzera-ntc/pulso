package com.organizador.agenda.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class RecurrenceUnit { Daily, Weekly, Monthly, Yearly }

/**
 * Repetição de uma tarefa. As ocorrências contam a partir de uma âncora (a data de vencimento
 * atual da tarefa), de [interval] em [interval] unidades:
 *  - [RecurrenceUnit.Weekly]: nos [weekdays] das semanas válidas (semanas de segunda a domingo);
 *  - [RecurrenceUnit.Monthly]: no dia [monthDay] (no último dia, se o mês for mais curto);
 *  - [RecurrenceUnit.Yearly]: no mesmo dia e mês da âncora (29/02 vira 28/02 nos anos comuns).
 */
data class Recurrence(
    val unit: RecurrenceUnit,
    val interval: Int = 1,
    val weekdays: Set<DayOfWeek> = emptySet(),
    val monthDay: Int? = null,
) {
    init {
        require(interval in 1..MAX_INTERVAL) { "Intervalo fora do limite: $interval" }
        require(unit != RecurrenceUnit.Weekly || weekdays.isNotEmpty()) { "Semanal sem dias" }
        require(monthDay == null || monthDay in 1..31) { "Dia do mês inválido: $monthDay" }
    }

    /** Primeira ocorrência depois de [after] (exclusivo), na série ancorada em [anchor]. */
    fun nextAfter(after: LocalDate, anchor: LocalDate): LocalDate = when (unit) {
        RecurrenceUnit.Daily ->
            if (after < anchor) anchor
            else anchor.plusDays((ChronoUnit.DAYS.between(anchor, after) / interval + 1) * interval)
        RecurrenceUnit.Weekly -> {
            val anchorWeek = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            generateSequence(after.plusDays(1)) { it.plusDays(1) }
                .first { d ->
                    val weeks = ChronoUnit.WEEKS.between(anchorWeek, d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                    d.dayOfWeek in weekdays && weeks >= 0 && weeks % interval == 0L
                }
        }
        RecurrenceUnit.Monthly -> {
            val day = monthDay ?: anchor.dayOfMonth
            val anchorMonth = YearMonth.from(anchor)
            generateSequence(YearMonth.from(after)) { it.plusMonths(1) }
                .filter { ChronoUnit.MONTHS.between(anchorMonth, it).let { m -> m >= 0 && m % interval == 0L } }
                .map { it.atDay(minOf(day, it.lengthOfMonth())) }
                .first { it > after }
        }
        RecurrenceUnit.Yearly -> {
            generateSequence(after.year) { it + 1 }
                .filter { y -> (y - anchor.year).let { it >= 0 && it % interval == 0 } }
                .map { y -> YearMonth.of(y, anchor.month).let { it.atDay(minOf(anchor.dayOfMonth, it.lengthOfMonth())) } }
                .first { it > after }
        }
    }

    /** Primeira data que cai na regra a partir de [from] (inclusive): para criar a tarefa já no dia certo. */
    fun firstOnOrAfter(from: LocalDate): LocalDate = when (unit) {
        RecurrenceUnit.Daily, RecurrenceUnit.Yearly -> from
        RecurrenceUnit.Weekly -> generateSequence(from) { it.plusDays(1) }.first { it.dayOfWeek in weekdays }
        RecurrenceUnit.Monthly -> {
            val day = monthDay ?: from.dayOfMonth
            generateSequence(YearMonth.from(from)) { it.plusMonths(1) }
                .map { it.atDay(minOf(day, it.lengthOfMonth())) }
                .first { it >= from }
        }
    }

    /** A mesma regra presa a uma data: completa os dias da semana / do mês que faltarem. */
    fun anchoredOn(date: LocalDate): Recurrence = when (unit) {
        RecurrenceUnit.Weekly -> this
        RecurrenceUnit.Monthly -> if (monthDay == null) copy(monthDay = date.dayOfMonth) else this
        else -> this
    }

    /** "D|1", "W|1|1,3,5", "M|1|5", "Y|1": guardado numa coluna de texto. */
    fun encode(): String = when (unit) {
        RecurrenceUnit.Daily -> "D|$interval"
        RecurrenceUnit.Weekly -> "W|$interval|" + weekdays.map { it.value }.sorted().joinToString(",")
        RecurrenceUnit.Monthly -> "M|$interval|" + (monthDay ?: "")
        RecurrenceUnit.Yearly -> "Y|$interval"
    }

    companion object {
        const val MAX_INTERVAL = 99
        val WORKDAYS: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

        fun daily(interval: Int = 1) = Recurrence(RecurrenceUnit.Daily, interval)
        fun weekly(vararg days: DayOfWeek, interval: Int = 1) = Recurrence(RecurrenceUnit.Weekly, interval, days.toSet())
        fun monthly(day: Int?, interval: Int = 1) = Recurrence(RecurrenceUnit.Monthly, interval, monthDay = day)
        fun yearly(interval: Int = 1) = Recurrence(RecurrenceUnit.Yearly, interval)

        /** Inverso de [encode]; texto inválido (de uma versão futura, por ex.) vira null. */
        fun decode(text: String?): Recurrence? = runCatching {
            if (text.isNullOrBlank()) return null
            val parts = text.split('|')
            val interval = parts[1].toInt()
            when (parts[0]) {
                "D" -> daily(interval)
                "W" -> Recurrence(RecurrenceUnit.Weekly, interval, parts[2].split(',').map { DayOfWeek.of(it.toInt()) }.toSet())
                "M" -> monthly(parts.getOrNull(2)?.toIntOrNull(), interval)
                "Y" -> yearly(interval)
                else -> null
            }
        }.getOrNull()
    }
}
