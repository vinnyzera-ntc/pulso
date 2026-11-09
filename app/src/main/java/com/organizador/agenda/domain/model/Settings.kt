package com.organizador.agenda.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class FocusSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
)

data class AppSettings(
    /** null = ainda não configurado: mostra as agendas visíveis no app de agenda do sistema. */
    val visibleCalendarIds: Set<Long>? = null,
    /** null = escolha automática (agenda principal da conta Google). */
    val defaultCalendarId: Long? = null,
    val focus: FocusSettings = FocusSettings(),
    /** Horário do lembrete de tarefas com data mas sem hora. */
    val taskReminderTime: LocalTime = LocalTime.of(9, 0),
    /** Lembrete padrão de novos compromissos, em minutos antes. */
    val eventReminderMinutes: Int = 10,
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    /** Permissões já pedidas ao menos uma vez (para distinguir "nunca pedida" de "bloqueada"). */
    val requestedPermissions: Set<String> = emptySet(),
)
