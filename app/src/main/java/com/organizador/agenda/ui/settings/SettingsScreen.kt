package com.organizador.agenda.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.pulso.components.RoundIconButton

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    // Ajustes: sem aurora.
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.padding(start = 6.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), onBack)
        }
        ScreenHeader(title = stringResource(R.string.settings_title), modifier = Modifier.padding(top = 0.dp))
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.placeholder_settings_title),
                message = stringResource(R.string.placeholder_settings_body),
            )
        }
    }
}
