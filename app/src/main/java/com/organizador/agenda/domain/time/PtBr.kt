package com.organizador.agenda.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Formatação de datas e horas em português do Brasil, 24h.
 * Tabelas próprias em vez de DateTimeFormatter para não depender de variações do CLDR
 * entre versões do Android ("nov." com ponto, "segunda-feira" etc.).
 */
object PtBr {
    val locale: Locale = Locale.forLanguageTag("pt-BR")

    private val weekdays = mapOf(
        DayOfWeek.MONDAY to "segunda", DayOfWeek.TUESDAY to "terça", DayOfWeek.WEDNESDAY to "quarta",
        DayOfWeek.THURSDAY to "quinta", DayOfWeek.FRIDAY to "sexta", DayOfWeek.SATURDAY to "sábado",
        DayOfWeek.SUNDAY to "domingo",
    )
    private val monthsFull = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )

    /** "segunda", "terça"… */
    fun weekday(day: DayOfWeek): String = weekdays.getValue(day)

    /** "seg", "ter", "qua", "qui", "sex", "sáb", "dom". */
    fun weekdayShort(day: DayOfWeek): String = weekday(day).take(3)

    /** "novembro". */
    fun month(month: Month): String = monthsFull[month.ordinal]

    /** "nov". */
    fun monthShort(month: Month): String = month(month).take(3)

    /** Eyebrow do Hoje, em sentence case: "Segunda · 9 nov". O SectionLabel/eyebrow põe em caixa alta. */
    fun eyebrowDate(date: LocalDate): String =
        "${weekday(date.dayOfWeek).capitalized()} · ${date.dayOfMonth} ${monthShort(date.month)}"

    /** "Ter, 10 nov". */
    fun shortDate(date: LocalDate): String =
        "${weekdayShort(date.dayOfWeek).capitalized()}, ${date.dayOfMonth} ${monthShort(date.month)}"

    /** "14:00", "09:05". */
    fun time(t: LocalTime): String = "%02d:%02d".format(t.hour, t.minute)

    /** Saudação pelo horário: 05–11 bom dia, 12–17 boa tarde, resto boa noite. */
    fun greeting(t: LocalTime): String = when (t.hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    /** "25 min", "1 h", "1 h 30 min". */
    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "$m min"
            m == 0 -> "$h h"
            else -> "$h h $m min"
        }
    }

    /** Lembrete relativo: "Na hora", "10 min antes", "1 h antes". */
    fun reminderBefore(minutes: Int): String = when {
        minutes <= 0 -> "Na hora"
        minutes % 1440 == 0 -> if (minutes == 1440) "1 dia antes" else "${minutes / 1440} dias antes"
        else -> "${duration(minutes)} antes"
    }

    /**
     * Dia relativo a hoje, em minúsculas: "hoje", "amanhã", "ontem", "há 3 dias" (até 6),
     * senão a data curta ("ter, 10 nov"; com ano se for outro ano).
     */
    fun relativeDay(date: LocalDate, today: LocalDate): String =
        when (val days = ChronoUnit.DAYS.between(today, date).toInt()) {
            0 -> "hoje"
            1 -> "amanhã"
            -1 -> "ontem"
            in -6..-2 -> "há ${-days} dias"
            else -> shortDate(date).lowercase(locale) + if (date.year != today.year) " ${date.year}" else ""
        }

    /** "em 25 min", "em 1 h 20 min"; "agora" para zero ou negativo. */
    fun inMinutes(minutes: Long): String = if (minutes <= 0) "agora" else "em ${duration(minutes.toInt())}"

    fun String.capitalized(): String = replaceFirstChar { it.titlecase(locale) }
}
