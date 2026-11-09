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
                PulsoNavHost(startRoute = debugStartRoute())
            }
        }
    }

    /**
     * Só no build debug: `adb shell am start -n com.organizador.agenda/.MainActivity -e tela agenda`
     * abre direto numa aba (ou em "ajustes"). Usado pelo script de prints (scripts/prints.sh).
     */
    private fun debugStartRoute(): Any? {
        if (!BuildConfig.DEBUG) return null
        val key = intent?.getStringExtra(EXTRA_SCREEN) ?: return null
        if (key == "ajustes") return SettingsRoute
        return TopLevel.entries.firstOrNull { it.key == key }?.route
    }

    companion object {
        const val EXTRA_SCREEN = "tela"
    }
}
