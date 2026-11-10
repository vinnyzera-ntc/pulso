package com.organizador.agenda

import android.os.Bundle
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
                PulsoNavHost(startRoute = debugStartRoute(), seedMode = debugExtra(EXTRA_SEED))
            }
        }
    }

    /**
     * Só no build debug: `adb shell am start -n com.organizador.agenda/.MainActivity -e tela agenda`
     * abre direto numa aba (ou em "ajustes"). Usado pelo script de prints (scripts/prints.sh).
     */
    private fun debugExtra(name: String): String? = if (BuildConfig.DEBUG) intent?.getStringExtra(name) else null

    private fun debugStartRoute(): Any? {
        val key = debugExtra(EXTRA_SCREEN) ?: return null
        if (key == "ajustes") return SettingsRoute
        return TopLevel.entries.firstOrNull { it.key == key }?.route
    }

    companion object {
        const val EXTRA_SCREEN = "tela"

        /** `-e seed demo | demo-proximo | limpar`: dados de exemplo (ver DebugSeeder). */
        const val EXTRA_SEED = "seed"
    }
}
