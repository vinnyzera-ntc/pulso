package com.organizador.agenda.domain.model

import java.time.DayOfWeek
import java.time.LocalTime

data class FocusSettings(
    val focusMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    // Opções da v1.2, todas desligadas por padrão.
    /** Tela sempre ligada durante a sessão. */
    val keepScreenOn: Boolean = false,
    /** Com o celular deitado, a sessão vira um descanso de tela (fundo preto, timer grande). */
    val landscapeScreensaver: Boolean = false,
    /** Liga o Não perturbe nas fases de foco (as pausas não). */
    val doNotDisturb: Boolean = false,
    /** Apps cujas notificações ainda tocam com o Não perturbe do foco (nomes de pacote). */
    val dndAllowedPackages: Set<String> = emptySet(),
)

data class AppSettings(
    /**
     * Escolhas do usuário por agenda (id -> visível). Agendas sem escolha seguem o "visível"
     * do app de agenda do sistema, então uma agenda nova aparece sem precisar configurar.
     */
    val calendarVisibility: Map<Long, Boolean> = emptyMap(),
    /** null = escolha automática (agenda principal da conta Google). */
    val defaultCalendarId: Long? = null,
    val focus: FocusSettings = FocusSettings(),
    /** Horário do lembrete de tarefas com data mas sem hora. */
    val taskReminderTime: LocalTime = LocalTime.of(9, 0),
    /** Lembrete padrão de novos compromissos, em minutos antes. */
    val eventReminderMinutes: Int = 10,
    val weekStart: DayOfWeek = DayOfWeek.SUNDAY,
    /** Últimas frases da Captura rápida, da mais recente para a mais antiga. */
    val recentCaptures: List<String> = emptyList(),
    /** Permissões já pedidas ao menos uma vez (para distinguir "nunca pedida" de "bloqueada"). */
    val requestedPermissions: Set<String> = emptySet(),
    /** Quantos dias depois de hoje aparecem em "Próximos dias" (Hoje e widget). */
    val upcomingDays: Int = DEFAULT_UPCOMING_DAYS,
) {
    companion object {
        const val DEFAULT_UPCOMING_DAYS = 7
    }
}
