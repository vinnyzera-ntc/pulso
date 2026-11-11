package com.organizador.agenda.domain.calendar

import com.organizador.agenda.domain.model.Attendee
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.model.EventDraft
import com.organizador.agenda.domain.time.PtBr
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** O que a folha de compromisso edita. Puro; vira [EventDraft] para gravar. */
data class EventForm(
    val title: String = "",
    val calendarId: Long? = null,
    val date: LocalDate,
    val time: LocalTime,
    val durationMinutes: Int = 30,
    val location: String = "",
    val description: String = "",
    /** Participantes de um evento que já existia (o Pulso não adiciona nem remove; só preserva ao editar). */
    val attendees: List<Attendee> = emptyList(),
    /** Minutos antes; null = sem lembrete. */
    val reminderMinutes: Int? = null,
    /** Lembretes do evento original que a folha não mostra (ao editar, são preservados). */
    val extraReminders: List<Int> = emptyList(),
) {
    val isValid: Boolean get() = title.isNotBlank() && calendarId != null && durationMinutes in 1..MAX_DURATION

    val start: LocalDateTime get() = date.atTime(time)
    val end: LocalDateTime get() = start.plusMinutes(durationMinutes.toLong())

    fun toDraft(zone: ZoneId): EventDraft = EventDraft(
        calendarId = requireNotNull(calendarId),
        title = title.trim(),
        start = start.atZone(zone).toInstant(),
        end = end.atZone(zone).toInstant(),
        timeZone = zone,
        location = location.trim(),
        description = description,
        attendees = attendees,
        reminderMinutes = (listOfNotNull(reminderMinutes) + extraReminders).distinct().sorted(),
    )

    companion object {
        const val MAX_DURATION = 24 * 60
        val DURATION_OPTIONS = listOf(15, 30, 45, 60)
        val REMINDER_OPTIONS = listOf<Int?>(null, 10, 30, 60)

        /** Início sugerido para um compromisso novo: a próxima meia hora cheia. */
        fun suggestedStart(now: LocalDateTime): LocalDateTime {
            val base = now.withSecond(0).withNano(0)
            val add = 30 - base.minute % 30
            return base.plusMinutes(add.toLong())
        }

        fun new(start: LocalDateTime, calendarId: Long?, reminderMinutes: Int?): EventForm =
            EventForm(calendarId = calendarId, date = start.toLocalDate(), time = start.toLocalTime(), reminderMinutes = reminderMinutes)

        /**
         * Formulário para editar a ocorrência [instanceStart]–[instanceEnd] de um evento.
         * O lembrete mostrado é o menor; os outros ficam em [extraReminders].
         */
        fun edit(details: EventDetails, instanceStart: Instant, instanceEnd: Instant, zone: ZoneId): EventForm {
            val start = LocalDateTime.ofInstant(instanceStart, zone)
            val reminders = details.reminderMinutes.sorted()
            return EventForm(
                title = details.event.title,
                calendarId = details.event.calendarId,
                date = start.toLocalDate(),
                time = start.toLocalTime(),
                durationMinutes = Duration.between(instanceStart, instanceEnd).toMinutes().toInt().coerceAtLeast(1),
                location = details.event.location.orEmpty(),
                description = details.description,
                attendees = details.attendees,
                reminderMinutes = reminders.firstOrNull(),
                extraReminders = reminders.drop(1),
            )
        }
    }
}

/** Textos da confirmação ("Compromisso agendado") e do botão, montados a partir do formulário. Puro. */
object EventSummary {
    /** "Ter, 10 nov · 14:00": o resultado em números. */
    fun value(form: EventForm): String = "${PtBr.shortDate(form.date)} · ${PtBr.time(form.time)}"

    /** "Com Ana e Bruno · 45 min · Sala 3". */
    fun meta(form: EventForm): String = listOfNotNull(
        people(form.attendees)?.let { "Com $it" },
        PtBr.duration(form.durationMinutes),
        form.location.trim().takeIf { it.isNotEmpty() },
    ).joinToString(" · ")

    /** "Ana", "Ana e Bruno", "Ana, Bruno e mais 2" (primeiros nomes). */
    fun people(attendees: List<Attendee>): String? {
        val names = attendees.map { it.name.substringBefore(" ").substringBefore("@") }
        return when (names.size) {
            0 -> null
            1 -> names[0]
            2 -> "${names[0]} e ${names[1]}"
            else -> "${names[0]}, ${names[1]} e mais ${names.size - 2}"
        }
    }

    fun reminder(form: EventForm): String? = form.reminderMinutes?.let { "Lembrete ${PtBr.reminderBefore(it).replaceFirstChar(Char::lowercase)}" }
}
