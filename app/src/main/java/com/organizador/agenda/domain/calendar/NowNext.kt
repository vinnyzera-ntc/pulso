package com.organizador.agenda.domain.calendar

import com.organizador.agenda.domain.model.CalendarEvent
import java.time.Duration
import java.time.Instant

/** O compromisso que o Hoje destaca: o que está acontecendo, ou o próximo do dia. */
sealed interface NowNext {
    /** Em andamento. [progress] é a fração decorrida (0..1). */
    data class Current(val event: CalendarEvent, val progress: Float, val minutesLeft: Long) : NowNext
    data class Next(val event: CalendarEvent, val minutesUntil: Long) : NowNext
    data object None : NowNext

    companion object {
        /** [events] são os eventos do dia. Eventos de dia inteiro não contam como "agora". */
        fun pick(events: List<CalendarEvent>, now: Instant): NowNext {
            val timed = events.filter { !it.allDay }
            // Se dois se sobrepõem, o que começou por último é o mais específico.
            val current = timed.filter { it.start <= now && now < it.end }.maxByOrNull { it.start }
            if (current != null) {
                val total = Duration.between(current.start, current.end).toMillis().coerceAtLeast(1)
                val elapsed = Duration.between(current.start, now).toMillis()
                return Current(current, (elapsed.toFloat() / total).coerceIn(0f, 1f), ceilMinutes(now, current.end))
            }
            val next = timed.filter { it.start > now }.minByOrNull { it.start } ?: return None
            return Next(next, ceilMinutes(now, next.start))
        }

        /** Minutos até [to], arredondando para cima: faltando 42min10s mostra 43. */
        private fun ceilMinutes(from: Instant, to: Instant): Long = (Duration.between(from, to).seconds + 59) / 60
    }
}
