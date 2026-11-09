package com.organizador.agenda.domain.calendar

import com.organizador.agenda.domain.model.DeviceCalendar

/** Regras de quais agendas mostrar e em qual criar. Puras, sem Android, testadas em JVM. */
object CalendarSelection {

    /**
     * Agendas a mostrar. Sem configuração do usuário, segue o "visível" do app de agenda do sistema.
     * Ids configurados que não existem mais (agenda removida) são descartados.
     */
    fun visibleIds(calendars: List<DeviceCalendar>, configured: Set<Long>?): Set<Long> {
        val existing = calendars.map { it.id }.toSet()
        return configured?.intersect(existing)
            ?: calendars.filter { it.visibleInSystem }.map { it.id }.toSet()
    }

    /** Novo conjunto após ligar/desligar uma agenda, partindo do estado efetivo atual. */
    fun toggle(calendars: List<DeviceCalendar>, configured: Set<Long>?, id: Long, visible: Boolean): Set<Long> {
        val current = visibleIds(calendars, configured)
        return if (visible) current + id else current - id
    }

    /**
     * Agenda onde criar compromissos: a escolhida pelo usuário, se ainda existir e aceitar escrita;
     * senão a principal de uma conta Google; senão qualquer Google; senão a primeira com escrita.
     */
    fun defaultForCreate(calendars: List<DeviceCalendar>, configured: Long?): DeviceCalendar? {
        val writable = calendars.filter { it.canWrite }
        writable.firstOrNull { it.id == configured }?.let { return it }
        return writable.minWithOrNull(
            compareBy<DeviceCalendar>(
                { !(it.isGoogle && it.isPrimary) },
                { !it.isGoogle },
                { !it.isPrimary },
                { it.displayName.lowercase() },
            ),
        )
    }
}
