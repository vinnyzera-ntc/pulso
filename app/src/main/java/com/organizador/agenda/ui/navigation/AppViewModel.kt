package com.organizador.agenda.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.organizador.agenda.BuildConfig
import com.organizador.agenda.debug.DebugSeeder
import com.organizador.agenda.ui.common.Messenger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Estado no nível do app: o canal de mensagens (snackbar) e o seeder de debug. */
@HiltViewModel
class AppViewModel @Inject constructor(
    val messenger: Messenger,
    private val seeder: DebugSeeder,
) : ViewModel() {
    private var seeded = false

    /** Só em build debug, uma vez por processo de tela (não repete ao girar ou trocar de tema). */
    fun seedOnce(mode: String) {
        if (!BuildConfig.DEBUG || seeded) return
        seeded = true
        viewModelScope.launch { seeder.run(mode) }
    }
}
