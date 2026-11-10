package com.organizador.agenda.domain.calendar

import com.organizador.agenda.domain.model.DeviceCalendar

/** Regras de quais agendas mostrar e em qual criar. Puras, sem Android, testadas em JVM. */
object CalendarSelection {

    /**
     * Agendas a mostrar. Cada agenda segue a escolha do usuário, se houver; senão, o "visível"
     * do app de agenda do sistema. Escolhas de agendas que não existem mais são ignoradas.
     */
    fun visibleIds(calendars: List<DeviceCalendar>, overrides: Map<Long, Boolean>): Set<Long> =
        calendars.filter { overrides[it.id] ?: it.visibleInSystem }.map { it.id }.toSet()

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
