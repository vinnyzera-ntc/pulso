package com.organizador.agenda.ui.focus

/**
 * Ações da sessão de foco em andamento. As telas (sessão, descanso de tela) dependem só disto,
 * não do ViewModel do Hilt: dá para desenhar em preview e testar com um falso.
 */
interface FocusActions {
    fun pause()
    fun resume()
    fun startBreak()
    fun nextFocus()
    fun stop()
    fun completeTask()
}
