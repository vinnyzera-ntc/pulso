package com.organizador.agenda.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.organizador.agenda.R
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoTheme

@Composable
fun FocusScreen(contentPadding: PaddingValues) {
    val c = Pulso.colors
    // O modo foco usa o fundo `deep`.
    Column(
        Modifier
            .fillMaxSize()
            .background(c.deep)
            .statusBarsPadding()
            .padding(contentPadding),
    ) {
        ScreenHeader(title = stringResource(R.string.nav_focus), eyebrow = stringResource(R.string.focus_eyebrow))
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.placeholder_focus_title),
                message = stringResource(R.string.placeholder_focus_body),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun FocusPreview() = PulsoTheme(darkTheme = true) { FocusScreen(PaddingValues()) }
