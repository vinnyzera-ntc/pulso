package com.organizador.agenda.focus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A tarefa escolhida para a próxima sessão de foco. Fica fora da tela do Foco para que outras
 * telas possam pré-selecionar ("Focar nesta tarefa" no detalhe) antes de navegar até ela.
 */
@Singleton
class FocusSelection @Inject constructor() {
    private val state = MutableStateFlow<Long?>(null)
    val taskId: StateFlow<Long?> = state

    fun select(taskId: Long?) {
        state.value = taskId
    }
}
