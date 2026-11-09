package com.organizador.agenda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.organizador.agenda.ui.navigation.PulsoNavHost
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
                PulsoNavHost(startTab = debugStartTab())
            }
        }
    }

    /**
     * Só no build debug: `adb shell am start -n com.organizador.agenda/.MainActivity -e tela agenda`
     * abre direto numa aba. Usado pelo script de prints (scripts/prints.sh).
     */
    private fun debugStartTab(): TopLevel? {
        if (!BuildConfig.DEBUG) return null
        val key = intent?.getStringExtra(EXTRA_SCREEN) ?: return null
        return TopLevel.entries.firstOrNull { it.key == key }
    }

    companion object {
        const val EXTRA_SCREEN = "tela"
    }
}
