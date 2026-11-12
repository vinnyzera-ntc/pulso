package com.organizador.agenda

import android.os.Bundle
import androidx.compose.runtime.mutableIntStateOf
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.organizador.agenda.ui.navigation.PulsoNavHost
import com.organizador.agenda.ui.navigation.SettingsRoute
import com.organizador.agenda.ui.navigation.TopLevel
import com.organizador.agenda.ui.pulso.PulsoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PulsoTheme {
                PulsoNavHost(startRoute = startRoute(), seedMode = debugExtra(EXTRA_SEED), openFocusSignal = openFocusSignal.intValue)
            }
        }
    }

    private val openFocusSignal = mutableIntStateOf(0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra(EXTRA_OPEN) == OPEN_FOCUS) openFocusSignal.intValue++
    }

    /**
     * Só no build debug: `adb shell am start -n com.organizador.agenda/.MainActivity -e tela agenda`
     * abre direto numa aba (ou em "ajustes"). Usado pelo script de prints (scripts/prints.sh).
     */
    private fun debugExtra(name: String): String? = if (BuildConfig.DEBUG) intent?.getStringExtra(name) else null

    /** Rota inicial: a pedida pela notificação do foco, ou o atalho de debug. */
    private fun startRoute(): Any? =
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_FOCUS) TopLevel.Focus.route else debugStartRoute()

    private fun debugStartRoute(): Any? {
        val key = debugExtra(EXTRA_SCREEN) ?: return null
        if (key == "ajustes") return SettingsRoute
        return TopLevel.entries.firstOrNull { it.key == key }?.route
    }

    companion object {
        const val EXTRA_SCREEN = "tela"

        /** Extra da notificação do modo foco: abre o app na aba Foco. */
        const val EXTRA_OPEN = "abrir"
        const val OPEN_FOCUS = "foco"

        /** `-e seed demo | demo-proximo | limpar`: dados de exemplo (ver DebugSeeder). */
        const val EXTRA_SEED = "seed"
    }
}
