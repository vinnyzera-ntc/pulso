package com.organizador.agenda.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.BuildConfig
import com.organizador.agenda.debug.DebugSeeder
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import com.organizador.agenda.focus.FocusController
import com.organizador.agenda.ui.common.Messenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado no nível do app: o canal de mensagens (snackbar) e o seeder de debug. */
@HiltViewModel
class AppViewModel @Inject constructor(
    val messenger: Messenger,
    private val seeder: DebugSeeder,
    focus: FocusController,
) : ViewModel() {
    /** Há uma sessão de foco ativa (o nav pill some na aba Foco enquanto isso). */
    val focusActive: StateFlow<Boolean> =
        focus.state.map { it.active }.stateIn(viewModelScope, SharingStarted.Eagerly, focus.state.value.active)

    private var seeded = false

    /** Só em build debug, uma vez por processo de tela (não repete ao girar ou trocar de tema). */
    fun seedOnce(mode: String) {
        if (!BuildConfig.DEBUG || seeded) return
        seeded = true
        viewModelScope.launch { seeder.run(mode) }
    }
}
