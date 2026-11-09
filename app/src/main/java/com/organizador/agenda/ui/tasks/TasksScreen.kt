package com.organizador.agenda.ui.tasks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.organizador.agenda.R
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.pulso.PulsoTheme
import com.organizador.agenda.ui.pulso.components.RoundIconButton

@Composable
fun TasksScreen(contentPadding: PaddingValues) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(contentPadding)) {
        ScreenHeader(title = stringResource(R.string.nav_tasks)) {
            RoundIconButton(Icons.Outlined.Search, stringResource(R.string.action_search), onClick = {})
        }
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.placeholder_tasks_title),
                message = stringResource(R.string.placeholder_tasks_body),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun TasksPreview() = PulsoTheme(darkTheme = false) { TasksScreen(PaddingValues()) }
