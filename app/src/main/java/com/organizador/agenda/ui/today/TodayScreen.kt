package com.organizador.agenda.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoTheme
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.aurora

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenCapture: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val header by viewModel.header.collectAsStateWithLifecycle()
    TodayContent(header, contentPadding, onOpenSettings, onOpenCapture)
}

@Composable
private fun TodayContent(
    header: TodayHeader,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenCapture: () -> Unit,
) {
    val c = Pulso.colors
    // Hoje é tela hero: aurora ligada.
    Box(Modifier.fillMaxSize().aurora(c)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(contentPadding)) {
            ScreenHeader(title = header.greeting, eyebrow = header.eyebrow) {
                RoundIconButton(Icons.Outlined.AutoAwesome, stringResource(R.string.action_quick_capture), onOpenCapture)
                RoundIconButton(Icons.Outlined.Settings, stringResource(R.string.settings_title), onOpenSettings)
            }
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = stringResource(R.string.placeholder_today_title),
                    message = stringResource(R.string.placeholder_today_body),
                )
            }
        }
    }
}

@Preview(name = "Hoje · escuro", widthDp = 360, heightDp = 780)
@Composable
private fun TodayDarkPreview() = PulsoTheme(darkTheme = true) {
    TodayContent(TodayHeader("Segunda · 9 nov", "Boa tarde"), PaddingValues(), {}, {})
}

@Preview(name = "Hoje · claro", widthDp = 360, heightDp = 780)
@Composable
private fun TodayLightPreview() = PulsoTheme(darkTheme = false) {
    TodayContent(TodayHeader("Segunda · 9 nov", "Boa tarde"), PaddingValues(), {}, {})
}
