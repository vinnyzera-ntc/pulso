package com.organizador.agenda.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.model.Subtask
import com.organizador.agenda.domain.model.Task
import com.organizador.agenda.domain.task.TaskLists
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.ChoiceSheet
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.PulsoBottomSheet
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.components.ChipTone
import com.organizador.agenda.ui.pulso.components.LabelIcon
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.PriorityCheckbox
import com.organizador.agenda.ui.pulso.components.PulsoCard
import com.organizador.agenda.ui.pulso.components.PulsoChip
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.SurfaceLevel
import java.time.LocalDateTime

private enum class DetailSheet { When, Priority, Tags, Reminder }

@Composable
fun TaskDetailScreen(onBack: () -> Unit, viewModel: TaskDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.close.collect { onBack() } }

    // Detalhe: sem aurora.
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp).padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            RoundIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back), onBack)
            if (state.task != null) {
                RoundIconButton(Icons.Outlined.DeleteOutline, stringResource(R.string.task_delete), { viewModel.delete() })
            }
        }
        val task = state.task
        when {
            !state.loaded -> Unit
            task == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(stringResource(R.string.task_missing_title), stringResource(R.string.task_missing_body))
            }
            else -> TaskDetailContent(task, state, viewModel)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.TaskDetailContent(
    task: Task,
    state: TaskDetailUiState,
    vm: TaskDetailViewModel,
) {
    val c = Pulso.colors
    var sheet by rememberSaveable { mutableStateOf<DetailSheet?>(null) }
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    val latestTitle by rememberUpdatedState(title)
    val latestNotes by rememberUpdatedState(notes)
    // Grava o texto em edição ao sair da tela (voltar, trocar de app).
    DisposableEffect(task.id) {
        onDispose {
            vm.rename(latestTitle)
            vm.setNotes(latestNotes)
        }
    }

    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = PulsoSpace.gutter).padding(bottom = 20.dp),
    ) {
        // ---------- Título ----------
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            textStyle = Pulso.type.headline.copy(
                color = if (task.isDone) c.text3 else c.text,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
            ),
            cursorBrush = SolidColor(c.ember),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { vm.rename(title) }),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).onFocusChanged { if (!it.isFocused) vm.rename(latestTitle) },
        )

        // ---------- Chips: quando, prioridade, etiquetas ----------
        FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val due = whenChip(task, state.now)
            ChipButton(due.first, due.second, Icons.Outlined.CalendarMonth) { sheet = DetailSheet.When }
            ChipButton(
                if (task.priority == Priority.P4) stringResource(R.string.field_priority) else task.priority.name,
                ChipTone.Neutral, Icons.Outlined.Flag,
            ) { sheet = DetailSheet.Priority }
            if (task.tags.isEmpty()) {
                ChipButton(stringResource(R.string.field_tags), ChipTone.Neutral, Icons.Outlined.Sell) { sheet = DetailSheet.Tags }
            } else {
                task.tags.forEach { ChipButton("#${it.name}", ChipTone.Neutral, null) { sheet = DetailSheet.Tags } }
            }
        }

        // ---------- Subtarefas ----------
        SectionLabel(stringResource(R.string.task_subtasks), Modifier.padding(top = 18.dp, bottom = 8.dp), leading = { LabelIcon(Icons.Outlined.Checklist) })
        PulsoCard(Modifier.fillMaxWidth(), level = SurfaceLevel.Panel, contentPadding = PaddingValues(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            task.subtasks.forEachIndexed { i, sub ->
                androidx.compose.runtime.key(sub.id) {
                    SubtaskRow(
                        sub, first = i == 0, last = false,
                        onDone = { vm.setSubtaskDone(sub.id, it) },
                        onRename = { vm.renameSubtask(sub.id, it) },
                        onDelete = { vm.deleteSubtask(sub.id) },
                    )
                }
            }
            NewSubtaskRow(connect = task.subtasks.isNotEmpty(), onAdd = vm::addSubtask)
        }

        // ---------- Notas ----------
        SectionLabel(stringResource(R.string.task_notes), Modifier.padding(top = 18.dp, bottom = 8.dp), leading = { LabelIcon(Icons.AutoMirrored.Outlined.Notes) })
        PulsoCard(Modifier.fillMaxWidth(), level = SurfaceLevel.Panel) {
            PlainTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = stringResource(R.string.task_notes_placeholder),
                style = Pulso.type.bodyS,
                singleLine = false,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).onFocusChanged { if (!it.isFocused) vm.setNotes(latestNotes) },
            )
        }

        // ---------- Lembretes ----------
        SectionLabel(stringResource(R.string.task_reminders), Modifier.padding(top = 18.dp, bottom = 8.dp), leading = { LabelIcon(Icons.Outlined.NotificationsNone) })
        PulsoCard(Modifier.fillMaxWidth(), level = SurfaceLevel.Panel, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
            if (task.dueDate == null) {
                Text(stringResource(R.string.task_reminders_need_date), style = Pulso.type.bodyS, color = c.text2, modifier = Modifier.padding(vertical = 10.dp))
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    task.reminders.forEach { r ->
                        val label = PtBr.reminderBefore(r.offsetMinutes)
                        ChipButton(label, ChipTone.Volt, Icons.Outlined.Close, stringResource(R.string.task_remove_reminder, label)) {
                            vm.toggleReminder(r.offsetMinutes)
                        }
                    }
                    if (task.reminders.size < TaskDetailViewModel.REMINDER_OPTIONS.size) {
                        ChipButton(stringResource(R.string.task_add_reminder), ChipTone.Neutral, Icons.Outlined.Add) { sheet = DetailSheet.Reminder }
                    }
                }
            }
        }
    }

    // ---------- Ação principal ----------
    Box(Modifier.fillMaxWidth().padding(horizontal = PulsoSpace.gutter).padding(top = 8.dp, bottom = 12.dp)) {
        if (task.isDone) {
            SecondaryButton(stringResource(R.string.task_reopen), { vm.setDone(false) }, Modifier.fillMaxWidth())
        } else {
            PrimaryButton(stringResource(R.string.task_complete), { vm.setDone(true) }, Modifier.fillMaxWidth())
        }
    }

    val dismiss = { sheet = null }
    when (sheet) {
        DetailSheet.When -> PulsoBottomSheet(dismiss) {
            SectionLabel(stringResource(R.string.field_when), leading = { LabelIcon(Icons.Outlined.CalendarMonth) })
            WhenEditor(task.dueDate, task.dueTime, state.now.toLocalDate(), onChange = vm::setWhen)
            SecondaryButton(stringResource(R.string.action_done), dismiss, Modifier.fillMaxWidth())
        }
        DetailSheet.Priority -> ChoiceSheet(
            stringResource(R.string.field_priority), Priority.entries, task.priority, { it.name }, vm::setPriority, dismiss,
            description = stringResource(R.string.priority_description),
        )
        DetailSheet.Tags -> PulsoBottomSheet(dismiss) {
            SectionLabel(stringResource(R.string.field_tags), leading = { LabelIcon(Icons.Outlined.Sell) })
            TagPicker(state.allTags, task.tags.map { it.name }.toSet(), onToggle = vm::toggleTag)
            SecondaryButton(stringResource(R.string.action_done), dismiss, Modifier.fillMaxWidth())
        }
        DetailSheet.Reminder -> {
            val active = task.reminders.map { it.offsetMinutes }.toSet()
            ChoiceSheet<Int>(
                stringResource(R.string.task_add_reminder), TaskDetailViewModel.REMINDER_OPTIONS.filter { it !in active }, null,
                { reminderShort(it) }, vm::toggleReminder, dismiss,
                description = stringResource(R.string.reminder_description),
            )
        }
        null -> Unit
    }
}

private fun reminderShort(minutes: Int): String = when {
    minutes == 0 -> "Na hora"
    minutes % 1440 == 0 -> if (minutes == 1440) "1 dia" else "${minutes / 1440} dias"
    else -> PtBr.duration(minutes)
}

/** Texto e tom do chip de prazo: flare se atrasada, volt se tem data (tempo), neutro sem data. */
@Composable
private fun whenChip(task: Task, now: LocalDateTime): Pair<String, ChipTone> {
    val date = task.dueDate ?: return stringResource(R.string.when_no_date) to ChipTone.Neutral
    val label = TaskUiMapper.whenLabel(date, task.dueTime, now.toLocalDate())
    return when {
        TaskLists.isOverdue(task, now) -> stringResource(R.string.task_overdue_prefix, label.replaceFirstChar { it.lowercase() }) to ChipTone.Flare
        else -> label to ChipTone.Volt
    }
}

/** Chip do kit com alvo de toque de 48dp. */
@Composable
private fun ChipButton(text: String, tone: ChipTone, icon: ImageVector?, onClickLabel: String? = null, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = 48.dp).clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { PulsoChip(text, tone = tone, icon = icon) }
}

/** Campo sem moldura (o painel em volta já é a superfície), com cursor ember. */
@Composable
private fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    strike: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
) {
    val c = Pulso.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style.copy(color = if (strike) c.text3 else c.text, textDecoration = if (strike) TextDecoration.LineThrough else null),
        cursorBrush = SolidColor(c.ember),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
        modifier = modifier,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style, color = c.text3)
                inner()
            }
        },
    )
}

/** Hairline vertical ligando os checkboxes das subtarefas (a "trilha" na vertical). */
private fun Modifier.connector(color: androidx.compose.ui.graphics.Color, above: Boolean, below: Boolean) = drawBehind {
    val x = 24.dp.toPx()
    val center = 24.dp.toPx()
    val gap = 14.dp.toPx()
    if (above) drawLine(color, Offset(x, 0f), Offset(x, center - gap), 1.dp.toPx())
    if (below) drawLine(color, Offset(x, center + gap), Offset(x, size.height), 1.dp.toPx())
}

@Composable
private fun SubtaskRow(
    sub: Subtask,
    first: Boolean,
    last: Boolean,
    onDone: (Boolean) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val c = Pulso.colors
    val focusManager = LocalFocusManager.current
    var text by rememberSaveable(sub.id) { mutableStateOf(sub.title) }
    val latest by rememberUpdatedState(text)
    val saved by rememberUpdatedState(sub.title)
    val commit = { if (latest.trim() != saved) onRename(latest) }
    DisposableEffect(sub.id) { onDispose { commit() } }
    Row(Modifier.fillMaxWidth().connector(c.lineStrong, above = !first, below = !last), verticalAlignment = Alignment.CenterVertically) {
        PriorityCheckbox(sub.done, onDone, contentDescription = sub.title)
        PlainTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = "",
            style = Pulso.type.body,
            strike = sub.done,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            modifier = Modifier.weight(1f).onFocusChanged { if (!it.isFocused) commit() },
        )
        Box(Modifier.size(48.dp).clickable(role = Role.Button, onClick = onDelete), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Close, stringResource(R.string.task_remove_subtask, sub.title), tint = c.text3, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun NewSubtaskRow(connect: Boolean, onAdd: (String) -> Unit) {
    val c = Pulso.colors
    var text by rememberSaveable { mutableStateOf("") }
    Row(Modifier.fillMaxWidth().connector(c.lineStrong, above = connect, below = false), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Add, null, tint = c.text3, modifier = Modifier.size(18.dp))
        }
        PlainTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(R.string.task_add_subtask),
            style = Pulso.type.body,
            imeAction = ImeAction.Done,
            onImeAction = {
                onAdd(text)
                text = ""
            },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        )
        Spacer(Modifier.size(14.dp))
    }
}
