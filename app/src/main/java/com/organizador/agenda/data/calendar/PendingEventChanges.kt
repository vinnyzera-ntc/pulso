package com.organizador.agenda.data.calendar

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cancelamentos "em espera": ao cancelar um compromisso ele some da tela na hora, mas só é
 * apagado do Calendar Provider quando o aviso com "Desfazer" expira. Assim, desfazer não
 * dispara e-mails de cancelamento e novo convite aos participantes.
 */
@Singleton
class PendingEventChanges @Inject constructor() {
    data class Hidden(val eventIds: Set<Long> = emptySet(), val instanceIds: Set<Long> = emptySet()) {
        fun hides(eventId: Long, instanceId: Long) = eventId in eventIds || instanceId in instanceIds
    }

    private val state = MutableStateFlow(Hidden())
    val hidden: StateFlow<Hidden> = state

    fun hideEvent(eventId: Long) = state.update { it.copy(eventIds = it.eventIds + eventId) }
    fun showEvent(eventId: Long) = state.update { it.copy(eventIds = it.eventIds - eventId) }
    fun hideInstance(instanceId: Long) = state.update { it.copy(instanceIds = it.instanceIds + instanceId) }
    fun showInstance(instanceId: Long) = state.update { it.copy(instanceIds = it.instanceIds - instanceId) }
}
