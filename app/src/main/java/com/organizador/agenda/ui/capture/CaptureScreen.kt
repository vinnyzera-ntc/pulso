package com.organizador.agenda.ui.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.domain.capture.CaptureKind
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.isCompactHeight
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.components.CaptureField
import com.organizador.agenda.ui.pulso.components.EmberFab
import com.organizador.agenda.ui.pulso.components.PulsoChip
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.aurora
import com.organizador.agenda.ui.tasks.vector

/**
 * Captura rápida (padrão da skill): tela hero com aurora mais forte, o campo que destaca o que
 * foi entendido, "Entendi assim" para conferir antes de salvar e as frases recentes para reusar.
 */
@Composable
fun CaptureScreen(onClose: () -> Unit, viewModel: CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = Pulso.colors
    val focus = remember { FocusRequester() }
    // O teclado abre na hora (em paisagem ele cobriria a tela: fica por conta do usuário).
    if (!isCompactHeight()) LaunchedEffect(Unit) { focus.requestFocus() }

    Box(Modifier.fillMaxSize().aurora(c, intensity = c.auroraIntensity + 0.25f)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(Icons.Outlined.Close, stringResource(R.string.action_close), onClose)
                Text(stringResource(R.string.action_quick_capture).uppercase(PtBr.locale), style = Pulso.type.eyebrow, color = c.text2)
                Spacer(Modifier.size(48.dp))
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PulsoSpace.gutter).padding(bottom = 24.dp),
            ) {
                Text(stringResource(R.string.capture_prompt), style = Pulso.type.headline, color = c.text, modifier = Modifier.padding(top = 10.dp, bottom = 16.dp))

                CaptureField(
                    value = state.text,
                    onValueChange = viewModel::setText,
                    recognized = state.highlights,
                    placeholder = stringResource(R.string.capture_placeholder),
                    modifier = Modifier.focusRequester(focus),
                    footer = {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            KindChip(state.kind, enabled = state.canToggleKind, onToggle = viewModel::toggleKind)
                            Spacer(Modifier.weight(1f))
                            // A única ação ember da tela: enviar.
                            EmberFab(
                                onClick = viewModel::send,
                                contentDescription = stringResource(
                                    if (state.kind == CaptureKind.Event) R.string.capture_send_event else R.string.capture_send_task,
                                ),
                                icon = Icons.Rounded.ArrowUpward,
                                size = 48.dp,
                                modifier = Modifier.alpha(if (state.canSend) 1f else 0.45f),
                            )
                        }
                    },
                )

                if (state.chips.isNotEmpty() || state.recurrenceIgnored) {
                    SectionLabel(stringResource(R.string.capture_understood), Modifier.padding(top = 20.dp, bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.chips.forEach { PulsoChip(it.text, tone = it.tone, icon = it.icon?.vector()) }
                    }
                    if (state.recurrenceIgnored) {
                        Text(stringResource(R.string.capture_recurrence_note), style = Pulso.type.bodyS, color = c.text2, modifier = Modifier.padding(top = 8.dp))
                    }
                }

                if (state.recents.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.capture_recents), Modifier.padding(top = 22.dp, bottom = 2.dp))
                    state.recents.forEach { recent ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(PulsoShapes.control).clickable(role = Role.Button) { viewModel.setText(recent) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.History, null, tint = c.text3, modifier = Modifier.size(18.dp))
                            Text(recent, style = Pulso.type.bodyS, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/** "Tarefa ⌄" / "Compromisso ⌄": o que será criado. Só alterna quando a frase tem horário. */
@Composable
private fun KindChip(kind: CaptureKind, enabled: Boolean, onToggle: () -> Unit) {
    val c = Pulso.colors
    val label = stringResource(if (kind == CaptureKind.Event) R.string.new_item_event else R.string.new_item_task)
    val hint = stringResource(R.string.capture_kind_toggle)
    Box(
        Modifier
            .heightIn(min = 48.dp)
            .then(if (enabled) Modifier.clickable(onClickLabel = hint, role = Role.Button, onClick = onToggle) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .heightIn(min = 34.dp)
                .background(c.surface3, PulsoShapes.quick)
                .border(1.dp, c.line, PulsoShapes.quick)
                .padding(start = 12.dp, end = if (enabled) 6.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = Pulso.type.meta, color = c.text)
            if (enabled) Icon(Icons.Rounded.KeyboardArrowDown, null, tint = c.text2, modifier = Modifier.size(18.dp))
        }
    }
}
