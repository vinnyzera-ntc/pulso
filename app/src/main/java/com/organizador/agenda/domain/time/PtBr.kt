package com.organizador.agenda.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
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

    fun String.capitalized(): String = replaceFirstChar { it.titlecase(locale) }
}
