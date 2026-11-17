package com.organizador.agenda.ui.focus

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.FocusKind
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.focus.FocusLockService
import com.organizador.agenda.ui.common.PulsoBottomSheet
import com.organizador.agenda.ui.common.findActivity
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoTheme
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import kotlinx.coroutines.delay

/** Tela sempre ligada enquanto este composable estiver na tela. */
@Composable
fun KeepScreenOnEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

/** Esconde as barras do sistema; um deslize da borda mostra por um instante. */
@Composable
private fun FullscreenEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/**
 * Deixa a tela do foco por cima da tela de bloqueio (e acende a tela ao voltar), para o cadeado
 * bloquear o celular sem esconder o timer. Desfeito ao sair do descanso de tela.
 */
@Composable
private fun ShowOverLockScreenEffect() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose { }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity.setShowWhenLocked(false)
                activity.setTurnScreenOn(false)
            } else {
                @Suppress("DEPRECATION")
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            }
        }
    }
}

/** Posições do timer contra marcas na tela (burn-in): troca a cada minuto. */
private val BURN_IN_STEPS = listOf(
    DpOffset(0.dp, 0.dp), DpOffset((-22).dp, (-12).dp), DpOffset(22.dp, (-14).dp),
    DpOffset(18.dp, 12.dp), DpOffset((-18).dp, 14.dp), DpOffset(0.dp, (-18).dp),
)
private const val BURN_IN_STEP_MS = 60_000L
private const val CONTROLS_VISIBLE_MS = 6_000L

/**
 * Descanso de tela do foco (celular deitado): fundo preto, timer grande, fase e tarefa discretas.
 * Tocar mostra os controles por alguns segundos; com a fase concluída, eles ficam visíveis.
 */
@Composable
fun FocusScreensaver(
    state: FocusUiState,
    actions: FocusActions,
    onLock: () -> Unit,
    onLofi: () -> Unit,
) {
    FullscreenEffect()
    KeepScreenOnEffect()
    ShowOverLockScreenEffect()

    // Cada toque reinicia a contagem até esconder os controles.
    var touches by remember { mutableIntStateOf(0) }
    var controlsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(touches) {
        if (touches == 0) return@LaunchedEffect
        controlsVisible = true
        delay(CONTROLS_VISIBLE_MS)
        controlsVisible = false
    }
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(BURN_IN_STEP_MS)
            step = (step + 1) % BURN_IN_STEPS.size
        }
    }
    val touched: () -> Unit = { touches++ }
    val showControls = controlsVisible || state.finished
    val hint = stringResource(R.string.focus_screensaver_hint)

    // Sempre no tema escuro: é um painel preto, mesmo com o app no claro.
    PulsoTheme(darkTheme = true) {
        val c = Pulso.colors
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = touched)
                .semantics { contentDescription = hint },
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val timerSize = with(LocalDensity.current) { (maxHeight * 0.34f).toSp() }
                val shift = BURN_IN_STEPS[step]
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(shift.x.roundToPx(), shift.y.roundToPx()) }
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(phaseLabel(state).uppercase(PtBr.locale), style = Pulso.type.eyebrow, color = c.text3)
                    if (state.finished) {
                        Text(
                            stringResource(if (state.phase == FocusKind.Focus) R.string.focus_session_done else R.string.focus_break_done),
                            style = Pulso.type.title,
                            color = c.auroraText,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    } else {
                        Text(
                            state.timeText,
                            style = Pulso.type.timer.copy(fontSize = timerSize, lineHeight = timerSize * 1.08f),
                            // Branco suave: menos brilho a noite inteira na tela.
                            color = c.text.copy(alpha = 0.86f),
                            maxLines = 1,
                        )
                    }
                    Text(
                        if (!state.running && !state.finished) stringResource(R.string.focus_paused)
                        else state.taskTitle ?: stringResource(R.string.focus_no_task),
                        style = Pulso.type.bodyS,
                        color = if (!state.running && !state.finished) c.voltText else c.text3,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            ) {
                ScreensaverControls(state, actions, onLock, onLofi, onInteract = touched)
            }
        }
    }
}

@Composable
private fun phaseLabel(state: FocusUiState): String = when (state.phase) {
    FocusKind.Focus -> stringResource(R.string.focus_phase_focus, state.session, state.sessionsPerCycle)
    FocusKind.ShortBreak -> stringResource(R.string.focus_phase_short_break)
    FocusKind.LongBreak -> stringResource(R.string.focus_phase_long_break)
}

/** Pausar/retomar, encerrar, cadeado e YouTube; com a fase concluída, os próximos passos. */
@Composable
private fun ScreensaverControls(
    state: FocusUiState,
    actions: FocusActions,
    onLock: () -> Unit,
    onLofi: () -> Unit,
    onInteract: () -> Unit,
) {
    val isFocus = state.phase == FocusKind.Focus
    fun tap(action: () -> Unit): () -> Unit = { onInteract(); action() }
    Row(
        Modifier.widthIn(max = 560.dp).padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.finished) {
            val (secondary, secondaryAction) = if (isFocus) {
                stringResource(
                    if (state.nextBreak == FocusKind.LongBreak) R.string.focus_take_long_break else R.string.focus_take_short_break,
                    PtBr.duration(state.nextBreakMinutes),
                ) to actions::startBreak
            } else {
                stringResource(R.string.focus_stop) to actions::stop
            }
            SecondaryButton(secondary, tap(secondaryAction), Modifier.widthIn(min = 150.dp))
            PrimaryButton(stringResource(R.string.focus_next_session), tap(actions::nextFocus), Modifier.widthIn(min = 150.dp))
        } else {
            if (state.running) RoundIconButton(Icons.Rounded.Pause, stringResource(R.string.focus_pause), tap(actions::pause))
            else RoundIconButton(Icons.Rounded.PlayArrow, stringResource(R.string.focus_resume), tap(actions::resume))
            RoundIconButton(Icons.Rounded.Stop, stringResource(R.string.focus_stop_session), tap(actions::stop))
        }
        if (FocusLockService.supported) RoundIconButton(Icons.Outlined.Lock, stringResource(R.string.focus_lock), tap(onLock))
        LofiButton(tap(onLofi))
    }
}

/** O cadeado precisa do serviço de acessibilidade: explica e leva às configurações. */
@Composable
fun LockHelpSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    PulsoBottomSheet(onDismiss) {
        SectionLabel(stringResource(R.string.focus_lock))
        Text(stringResource(R.string.focus_lock_help_body), style = Pulso.type.bodyS, color = Pulso.colors.text2)
        Text(stringResource(R.string.focus_lock_help_steps), style = Pulso.type.bodyS, color = Pulso.colors.text2)
        PrimaryButton(
            stringResource(R.string.focus_lock_open_settings),
            onClick = {
                context.startActivity(FocusLockService.settingsIntent())
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
