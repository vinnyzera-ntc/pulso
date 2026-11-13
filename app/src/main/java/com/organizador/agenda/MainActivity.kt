package com.organizador.agenda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.organizador.agenda.ui.navigation.OpenTaskRequest
import com.organizador.agenda.ui.navigation.PulsoNavHost
import com.organizador.agenda.ui.navigation.SettingsRoute
import com.organizador.agenda.ui.navigation.TopLevel
import com.organizador.agenda.ui.pulso.PulsoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val openFocusSignal = mutableIntStateOf(0)

    /** Tarefa pedida pela notificação de lembrete (um objeto novo a cada toque). */
    private val openTask = mutableStateOf<OpenTaskRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Recriada (rotação, processo restaurado): a navegação já volta sozinha, não reabre a tarefa.
        if (savedInstanceState == null) openTask.value = taskRequest(intent)
        setContent {
            PulsoTheme {
                PulsoNavHost(
                    startRoute = startRoute(),
                    seedMode = debugExtra(EXTRA_SEED),
                    openFocusSignal = openFocusSignal.intValue,
                    openTask = openTask.value,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.getStringExtra(EXTRA_OPEN)) {
            OPEN_FOCUS -> openFocusSignal.intValue++
            OPEN_TASK -> openTask.value = taskRequest(intent)
        }
    }

    /**
     * Reaberto pelos Recentes, o Android devolve a intent original da notificação (com
     * FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY): ela não deve abrir a tarefa ou o foco de novo.
     */
    private fun Intent.fromNotification(): Boolean = flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0

    private fun taskRequest(intent: Intent?): OpenTaskRequest? =
        intent?.takeIf { it.fromNotification() && it.getStringExtra(EXTRA_OPEN) == OPEN_TASK }
            ?.getLongExtra(EXTRA_TASK_ID, -1)?.takeIf { it > 0 }?.let(::OpenTaskRequest)

    /**
     * Só no build debug: `adb shell am start -n com.organizador.agenda/.MainActivity -e tela agenda`
     * abre direto numa aba (ou em "ajustes"). Usado pelo script de prints (scripts/prints.sh).
     */
    private fun debugExtra(name: String): String? = if (BuildConfig.DEBUG) intent?.getStringExtra(name) else null

    /** Rota inicial: a pedida pela notificação do foco, ou o atalho de debug. */
    private fun startRoute(): Any? =
        if (intent?.takeIf { it.fromNotification() }?.getStringExtra(EXTRA_OPEN) == OPEN_FOCUS) TopLevel.Focus.route else debugStartRoute()

    private fun debugStartRoute(): Any? {
        val key = debugExtra(EXTRA_SCREEN) ?: return null
        if (key == "ajustes") return SettingsRoute
        return TopLevel.entries.firstOrNull { it.key == key }?.route
    }

    companion object {
        const val EXTRA_SCREEN = "tela"

        /** Extra das notificações: abre o app na aba Foco ou no detalhe de uma tarefa. */
        const val EXTRA_OPEN = "abrir"
        const val OPEN_FOCUS = "foco"
        const val OPEN_TASK = "tarefa"
        const val EXTRA_TASK_ID = "tarefa_id"

        /** `-e seed demo | demo-proximo | limpar`: dados de exemplo (ver DebugSeeder). */
        const val EXTRA_SEED = "seed"
    }
}
