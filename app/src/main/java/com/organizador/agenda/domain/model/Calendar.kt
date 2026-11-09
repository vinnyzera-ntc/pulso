package com.organizador.agenda.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** Uma agenda do Calendar Provider (ex.: as agendas da conta Google configurada no aparelho). */
data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    /** Cor da agenda definida pelo usuário no Google Agenda (ARGB). É dado, não token de tema. */
    val color: Int,
    val isPrimary: Boolean,
    /** "Visível" no app de agenda do sistema. */
    val visibleInSystem: Boolean,
    val canWrite: Boolean,
) {
    val isGoogle: Boolean get() = accountType == GOOGLE_ACCOUNT_TYPE

    companion object {
        const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}

/** Uma ocorrência de evento (instância) num intervalo de tempo. */
data class CalendarEvent(
    val eventId: Long,
    /** _ID da instância; distingue ocorrências de um evento recorrente. */
    val instanceId: Long,
    val calendarId: Long,
    val title: String,
    val start: Instant,
    val end: Instant,
    val allDay: Boolean,
    val location: String?,
    val color: Int,
    val isRecurring: Boolean,
) {
    /** Dia de início. Eventos de dia inteiro são gravados em UTC pelo provider. */
    fun startDate(zone: ZoneId): LocalDate =
        if (allDay) start.atZone(ZoneOffset.UTC).toLocalDate() else start.atZone(zone).toLocalDate()
}

data class Attendee(val name: String, val email: String)

/** Dados completos de um evento, para a tela de edição. */
data class EventDetails(
    val event: CalendarEvent,
    val description: String,
    val timeZone: String,
    val attendees: List<Attendee>,
    val reminderMinutes: List<Int>,
    val rrule: String?,
)

/** O que o usuário preenche ao criar ou editar um compromisso. */
data class EventDraft(
    val calendarId: Long,
    val title: String,
    val start: Instant,
    val end: Instant,
    val allDay: Boolean = false,
    val timeZone: ZoneId = ZoneId.systemDefault(),
    val location: String = "",
    val description: String = "",
    val attendees: List<Attendee> = emptyList(),
    val reminderMinutes: List<Int> = emptyList(),
)
