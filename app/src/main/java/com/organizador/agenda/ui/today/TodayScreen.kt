package com.organizador.agenda.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.task.DayProgress
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.IconTile
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.PulsoTheme
import com.organizador.agenda.ui.pulso.components.ChipTone
import com.organizador.agenda.ui.pulso.components.LabelIcon
import com.organizador.agenda.ui.pulso.components.LiveDot
import com.organizador.agenda.ui.pulso.components.NowEventCard
import com.organizador.agenda.ui.pulso.components.Person
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.ProgressSegments
import com.organizador.agenda.ui.pulso.components.PulsoCard
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.aurora
import com.organizador.agenda.ui.tasks.ChipIcon
import com.organizador.agenda.ui.tasks.TaskChipUi
import com.organizador.agenda.ui.tasks.TaskRowItem
import com.organizador.agenda.ui.tasks.TaskRowUi

@Composable
fun TodayScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onPlanTomorrow: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    TodayContent(
        state, contentPadding, onOpenSettings, onOpenCapture, onOpenAgenda, onOpenTasks, onOpenTask, onPlanTomorrow,
        onSetDone = viewModel::setDone,
    )
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenAgenda: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenTask: (Long) -> Unit,
    onPlanTomorrow: () -> Unit,
    onSetDone: (Long, Boolean) -> Unit,
) {
    val c = Pulso.colors
    // Hoje é tela hero: aurora ligada.
    Box(Modifier.fillMaxSize().aurora(c)) {
        if (!state.loaded) return@Box
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
        ) {
            item(key = "header") {
                ScreenHeader(title = state.greeting, eyebrow = state.eyebrow) {
                    RoundIconButton(Icons.Outlined.AutoAwesome, stringResource(R.string.action_quick_capture), onOpenCapture)
                    RoundIconButton(Icons.Outlined.Settings, stringResource(R.string.settings_title), onOpenSettings)
                }
            }
            if (state.progress.total > 0) {
                item(key = "progress") { DayProgressBlock(state.progress, Modifier.padding(horizontal = PulsoSpace.gutter).padding(top = 12.dp)) }
            }

            // ---------- Agora / Próximo ----------
            val event = state.event
            if (event != null) {
                item(key = "event") {
                    Column(Modifier.padding(horizontal = PulsoSpace.gutter).padding(top = 22.dp)) {
                        SectionLabel(
                            stringResource(if (event.live) R.string.today_now else R.string.today_next),
                            leading = { if (event.live) LiveDot() else LabelIcon(Icons.Outlined.Schedule) },
                            actionLabel = stringResource(R.string.nav_agenda),
                            onAction = onOpenAgenda,
                        )
                        Spacer(Modifier.height(8.dp))
                        NowEventCard(
                            timeRange = event.timeRange,
                            remaining = event.relative,
                            title = event.title,
                            progress = event.progress,
                            location = event.location,
                            attendees = event.attendees.mapIndexed { i, name -> Person(i.toString(), name) },
                            onClick = onOpenAgenda,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else if (!state.calendarConnected) {
                item(key = "connect") {
                    Column(Modifier.padding(horizontal = PulsoSpace.gutter).padding(top = 22.dp)) {
                        SectionLabel(stringResource(R.string.nav_agenda), leading = { LabelIcon(Icons.Outlined.CalendarMonth) })
                        Spacer(Modifier.height(8.dp))
                        PulsoCard(Modifier.fillMaxWidth(), onClick = onOpenSettings) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Icons.Outlined.EventAvailable)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(stringResource(R.string.today_connect_title), style = Pulso.type.titleS, color = c.text)
                                    Text(stringResource(R.string.today_connect_body), style = Pulso.type.bodyS, color = c.text2)
                                }
                            }
                        }
                    }
                }
            }

            // ---------- Tarefas de hoje ----------
            item(key = "tasks-label") {
                SectionLabel(
                    stringResource(R.string.today_tasks),
                    leading = { LabelIcon(Icons.AutoMirrored.Outlined.List) },
                    actionLabel = stringResource(R.string.today_see_all),
                    onAction = onOpenTasks,
                    modifier = Modifier.padding(horizontal = PulsoSpace.gutter).padding(top = 22.dp),
                )
            }
            if (state.tasks.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        title = stringResource(R.string.today_empty_title),
                        message = stringResource(R.string.today_empty_body),
                        modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
                        action = { PrimaryButton(stringResource(R.string.today_plan_tomorrow), onPlanTomorrow, Modifier.fillMaxWidth()) },
                    )
                }
            }
            itemsIndexed(state.tasks, key = { _, t -> t.id }) { i, task ->
                Column(Modifier.animateItem().padding(horizontal = PulsoSpace.gutter)) {
                    TaskRowItem(task, onCheckedChange = { onSetDone(task.id, it) }, onClick = { onOpenTask(task.id) })
                    if (i < state.tasks.lastIndex) HorizontalDivider(color = c.line, modifier = Modifier.padding(start = 35.dp))
                }
            }
        }
    }
}

/** Progresso do dia: um segmento por tarefa (até 12; acima disso, barra única) + "5 de 8 tarefas … 62%". */
@Composable
private fun DayProgressBlock(progress: DayProgress, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    Column(modifier) {
        if (progress.total <= 12) {
            ProgressSegments(total = progress.total, done = progress.done)
        } else {
            Box(
                Modifier.fillMaxWidth().height(4.dp).drawBehind {
                    val r = CornerRadius(size.height)
                    drawRoundRect(c.surface4, cornerRadius = r)
                    drawRoundRect(c.aurora, size = Size(size.width * progress.done / progress.total, size.height), cornerRadius = r)
                },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                pluralStringResource(R.plurals.today_progress, progress.total, progress.done, progress.total),
                style = Pulso.type.meta,
                color = c.text2,
            )
            Text("${progress.percent}%", style = Pulso.type.numeric, color = c.text2)
        }
    }
}

private val previewState = TodayUiState(
    loaded = true,
    eyebrow = "Terça · 10 nov",
    greeting = "Boa tarde",
    progress = DayProgress(3, 7),
    event = EventCardUi(1, true, "14:00 – 15:00", "termina em 35 min", "Alinhamento semanal do projeto", "Sala 3", 0.42f, listOf("Ana Lima", "Bruno Sá")),
    tasks = listOf(
        TaskRowUi(1, "Revisar proposta do cliente", false, Priority.P1, listOf(TaskChipUi("Atrasada · ontem", ChipTone.Flare, ChipIcon.Flag), TaskChipUi("#trabalho"))),
        TaskRowUi(2, "Enviar relatório mensal", false, Priority.P2, listOf(TaskChipUi("16:00", ChipTone.Volt, ChipIcon.Clock))),
        TaskRowUi(3, "Pagar boleto do condomínio", true, Priority.P4, emptyList()),
    ),
)

@Preview(name = "Hoje · escuro", widthDp = 360, heightDp = 780)
@Composable
private fun TodayDarkPreview() = PulsoTheme(darkTheme = true) {
    TodayContent(previewState, PaddingValues(), {}, {}, {}, {}, {}, {}, { _, _ -> })
}

@Preview(name = "Hoje · claro", widthDp = 360, heightDp = 780)
@Composable
private fun TodayLightPreview() = PulsoTheme(darkTheme = false) {
    TodayContent(previewState.copy(event = null, calendarConnected = false, tasks = emptyList(), progress = DayProgress(0, 0)), PaddingValues(), {}, {}, {}, {}, {}, {}, { _, _ -> })
}
