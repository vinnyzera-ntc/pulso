package com.organizador.agenda.ui.focus

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.PermissionCopy
import com.organizador.agenda.ui.common.PermissionPanel
import com.organizador.agenda.ui.common.PermissionStatus
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.common.isCompactHeight
import com.organizador.agenda.ui.common.rememberPermissionRequest
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.components.FocusOrbit
import com.organizador.agenda.ui.pulso.components.LabelIcon
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.SuccessHalo

@Composable
fun FocusScreen(contentPadding: PaddingValues, viewModel: FocusViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = Pulso.colors
    // O modo foco usa o fundo `deep`.
    Box(Modifier.fillMaxSize().background(c.deep)) {
        when {
            !state.loaded -> Unit
            state.active -> FocusSession(state, viewModel)
            else -> FocusSetup(state, contentPadding, viewModel)
        }
    }
}

// ---------- Preparação: escolher a tarefa e iniciar ----------

@Composable
private fun FocusSetup(state: FocusUiState, contentPadding: PaddingValues, vm: FocusViewModel) {
    val c = Pulso.colors
    val s = state.settings
    val compact = isCompactHeight()
    val startLabel = stringResource(R.string.focus_start, PtBr.duration(s.focusMinutes))
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader(title = stringResource(R.string.nav_focus), eyebrow = stringResource(R.string.focus_eyebrow))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PulsoSpace.gutter),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectionLabel(stringResource(R.string.focus_task_label), Modifier.padding(top = 18.dp, bottom = 2.dp), leading = { LabelIcon(Icons.Outlined.TaskAlt) })
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TaskOption(stringResource(R.string.focus_no_task), state.selectedTaskId == null) { vm.select(null) }
                state.tasks.forEach { task -> TaskOption(task.title, task.id == state.selectedTaskId) { vm.select(task.id) } }
            }
            Text(
                stringResource(R.string.focus_plan, PtBr.duration(s.focusMinutes), PtBr.duration(s.shortBreakMinutes), PtBr.duration(s.longBreakMinutes), s.sessionsBeforeLongBreak),
                style = Pulso.type.bodyS,
                color = c.text2,
                modifier = Modifier.padding(top = 10.dp),
            )
            NotificationPermission(state, vm)
            Spacer(Modifier.height(12.dp))
            // Em tela baixa (paisagem) o botão rola junto, em vez de ficar fixo tomando a altura.
            if (compact) {
                PrimaryButton(startLabel, onClick = vm::start, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 12.dp))
            }
        }
        // Uma ação principal, com o resultado no rótulo.
        if (!compact) {
            PrimaryButton(
                startLabel,
                onClick = vm::start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PulsoSpace.gutter)
                    .padding(top = 8.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp),
            )
        }
    }
}

/** Opção de tarefa: seleção de valor, então o anel ember marca a escolhida. */
@Composable
private fun TaskOption(title: String, selected: Boolean, onClick: () -> Unit) {
    val c = Pulso.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(if (selected) c.emberSoft else c.surface1, PulsoShapes.control)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.ember else c.line, PulsoShapes.control)
            .clip(PulsoShapes.control)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = Pulso.type.body, color = if (selected) c.emberText else c.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Outlined.Check, null, tint = c.emberText, modifier = Modifier.size(20.dp))
    }
}

/** Android 13+: explica por que o app quer mostrar notificações; o foco funciona mesmo sem elas. */
@Composable
private fun NotificationPermission(state: FocusUiState, vm: FocusViewModel) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val request = rememberPermissionRequest(
        permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
        alreadyRequested = state.notificationPermissionRequested,
        onRequested = { vm.onNotificationPermissionRequested() },
        onResult = { },
    )
    if (request.status == PermissionStatus.Granted) return
    PermissionPanel(
        request = request,
        icon = Icons.Outlined.NotificationsNone,
        copy = PermissionCopy(
            title = stringResource(R.string.perm_notif_title),
            message = stringResource(R.string.perm_notif_message),
            requestLabel = stringResource(R.string.perm_notif_request),
            deniedTitle = stringResource(R.string.perm_notif_denied_title),
            deniedMessage = stringResource(R.string.perm_notif_denied_message),
            blockedTitle = stringResource(R.string.perm_notif_blocked_title),
            blockedMessage = stringResource(R.string.perm_notif_blocked_message),
            openSettingsLabel = stringResource(R.string.perm_open_settings),
        ),
        modifier = Modifier.padding(top = 10.dp),
        // O botão principal da tela é "Iniciar foco": aqui o pedido é secundário.
        primaryRequest = false,
    )
}

// ---------- Sessão em andamento ----------

@Composable
private fun FocusSession(state: FocusUiState, vm: FocusViewModel) {
    val c = Pulso.colors
    val phaseLabel = stringResource(
        when (state.phase) {
            FocusKind.Focus -> R.string.focus_eyebrow
            FocusKind.ShortBreak -> R.string.focus_phase_short_break
            FocusKind.LongBreak -> R.string.focus_phase_long_break
        },
    )
    Box(Modifier.fillMaxSize()) {
        if (isCompactHeight()) {
            // Paisagem: timer à esquerda, controles à direita (empilhados não cabem na altura).
            Row(Modifier.fillMaxSize()) {
                TimerPane(state, center = 0.5f, modifier = Modifier.weight(1f))
                Box(Modifier.weight(1f).fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.Center) {
                    SessionControls(state, vm, spacing = 10.dp)
                }
            }
        } else {
            TimerPane(state, center = 0.42f)
            SessionControls(state, vm, spacing = 14.dp, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp))
        }

        // Topo: "MODO FOCO … SESSÃO 2/4".
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = PulsoSpace.gutter).padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(phaseLabel.uppercase(PtBr.locale), style = Pulso.type.eyebrow, color = c.text2)
            Text(
                stringResource(R.string.focus_session_count, state.session, state.sessionsPerCycle).uppercase(PtBr.locale),
                style = Pulso.type.eyebrow,
                color = c.text2,
            )
        }
    }
}

/** O timer (ou o halo de conclusão) com a legenda logo abaixo: o que está em foco, ou o resultado. */
@Composable
private fun TimerPane(state: FocusUiState, center: Float, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    val isFocus = state.phase == FocusKind.Focus
    BoxWithConstraints(modifier.fillMaxSize()) {
        val pillCenter = maxHeight * center
        if (state.finished) {
            // Fim da fase: halo aurora no lugar do timer.
            Box(Modifier.align(Alignment.TopCenter).offset(y = pillCenter - 75.dp)) { SuccessHalo() }
        } else {
            FocusOrbit(timeText = state.timeText, running = state.running, centerFraction = center)
        }
        Column(
            Modifier.align(Alignment.TopCenter).offset(y = pillCenter + (if (state.finished) 82.dp else 54.dp)).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (state.finished) {
                Text(
                    stringResource(if (isFocus) R.string.focus_session_done else R.string.focus_break_done),
                    style = Pulso.type.body.copy(fontWeight = FontWeight.SemiBold),
                    color = c.auroraText,
                )
            } else {
                Text(
                    stringResource(
                        when {
                            !state.running -> R.string.focus_paused
                            isFocus -> R.string.focus_task_label
                            else -> R.string.focus_break_caption
                        },
                    ),
                    style = Pulso.type.meta,
                    color = if (state.running) c.text2 else c.voltText,
                )
            }
            Text(
                state.taskTitle ?: stringResource(R.string.focus_no_task),
                style = Pulso.type.titleS,
                color = c.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Segmentos das sessões + as ações da fase (uma só em ember) + "Encerrar foco". */
@Composable
private fun SessionControls(state: FocusUiState, vm: FocusViewModel, spacing: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    val isFocus = state.phase == FocusKind.Focus
    Column(
        modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = PulsoSpace.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing),
    ) {
        SessionSegments(state)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when {
                // Foco concluído: pausa (secundário) ou próxima sessão (principal).
                state.finished && isFocus -> {
                    val breakLabel = stringResource(
                        if (state.nextBreak == FocusKind.LongBreak) R.string.focus_take_long_break else R.string.focus_take_short_break,
                        PtBr.duration(state.nextBreakMinutes),
                    )
                    SecondaryButton(breakLabel, vm::startBreak, Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.focus_next_session), vm::nextFocus, Modifier.weight(1f))
                }
                state.finished -> {
                    SecondaryButton(stringResource(R.string.focus_stop), vm::stop, Modifier.weight(1f))
                    PrimaryButton(stringResource(R.string.focus_next_session), vm::nextFocus, Modifier.weight(1f))
                }
                else -> {
                    if (state.running) SecondaryButton(stringResource(R.string.focus_pause), vm::pause, Modifier.weight(1f))
                    else SecondaryButton(stringResource(R.string.focus_resume), vm::resume, Modifier.weight(1f))
                    when {
                        !isFocus -> PrimaryButton(stringResource(R.string.focus_back_to_focus), vm::nextFocus, Modifier.weight(1f))
                        state.taskTitle != null -> PrimaryButton(stringResource(R.string.task_complete), vm::completeTask, Modifier.weight(1f))
                        else -> SecondaryButton(stringResource(R.string.focus_stop), vm::stop, Modifier.weight(1f))
                    }
                }
            }
        }
        // "Encerrar" como ação de texto quando os dois botões já têm outro papel.
        val stopInButtons = (state.finished && !isFocus) || (!state.finished && isFocus && state.taskTitle == null)
        if (!stopInButtons) {
            Box(
                Modifier.heightIn(min = 48.dp).clip(PulsoShapes.chip).clickable(role = Role.Button, onClick = vm::stop).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.focus_stop_session), style = Pulso.type.meta, color = c.text2) }
        }
    }
}

/** Segmentos do ciclo: sessões feitas em aurora, a atual em volt (com brilho), as próximas em surface4. */
@Composable
private fun SessionSegments(state: FocusUiState) {
    val c = Pulso.colors
    val isFocus = state.phase == FocusKind.Focus
    // Durante a pausa (ou com o foco concluído), a sessão atual já conta como feita.
    val done = if (isFocus && !state.finished) state.session - 1 else state.session
    val description = stringResource(R.string.focus_session_count, state.session, state.sessionsPerCycle)
    Row(
        Modifier.widthIn(max = 220.dp).fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(state.sessionsPerCycle) { i ->
            val current = isFocus && !state.finished && i == state.session - 1
            Box(
                Modifier.weight(1f).height(4.dp).drawBehind {
                    val r = CornerRadius(size.height)
                    if (current) {
                        drawRoundRect(
                            c.voltGlow.copy(alpha = 0.35f),
                            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                            size = Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()),
                            cornerRadius = CornerRadius(size.height + 4.dp.toPx()),
                        )
                    }
                    drawRoundRect(if (i < done) c.aurora else if (current) c.volt else c.surface4, cornerRadius = r)
                },
            )
        }
    }
}
