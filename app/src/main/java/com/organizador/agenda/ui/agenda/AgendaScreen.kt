package com.organizador.agenda.ui.agenda

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.domain.time.PtBr.capitalized
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.pulso.PulsoTheme
import com.organizador.agenda.ui.pulso.components.SegmentedPills
import java.time.LocalDate
import java.time.temporal.IsoFields

@Composable
fun AgendaScreen(contentPadding: PaddingValues) {
    // Agenda não tem aurora (tela densa, utilitária).
    var view by rememberSaveable { mutableIntStateOf(1) }
    val today = remember { LocalDate.now() }
    val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(contentPadding)) {
        ScreenHeader(
            title = PtBr.month(today.month).capitalized(),
            eyebrow = stringResource(R.string.agenda_week_eyebrow, week),
        ) {
            SegmentedPills(
                options = listOf(
                    stringResource(R.string.agenda_view_day),
                    stringResource(R.string.agenda_view_week),
                    stringResource(R.string.agenda_view_month),
                ),
                selectedIndex = view,
                onSelect = { view = it },
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.placeholder_agenda_title),
                message = stringResource(R.string.placeholder_agenda_body),
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 780)
@Composable
private fun AgendaPreview() = PulsoTheme(darkTheme = true) { AgendaScreen(PaddingValues()) }
