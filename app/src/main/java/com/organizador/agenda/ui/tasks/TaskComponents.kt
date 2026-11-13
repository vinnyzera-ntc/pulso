package com.organizador.agenda.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.Priority as DomainPriority
import com.organizador.agenda.domain.model.Tag
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.FieldPanel
import com.organizador.agenda.ui.common.PulsoDatePickerDialog
import com.organizador.agenda.ui.common.PulsoTextField
import com.organizador.agenda.ui.common.PulsoTimePickerDialog
import com.organizador.agenda.ui.common.SelectableChip
import com.organizador.agenda.ui.common.useTimeLabel
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.components.Priority as RingPriority
import com.organizador.agenda.ui.pulso.components.PulsoChip
import com.organizador.agenda.ui.pulso.components.QuickChips
import com.organizador.agenda.ui.pulso.components.TaskRow
import java.time.LocalDate
import java.time.LocalTime

/** Prioridade do domínio -> anel do checkbox do kit (P4 = sem cor). */
fun DomainPriority.toRing(): RingPriority = when (this) {
    DomainPriority.P1 -> RingPriority.P1
    DomainPriority.P2 -> RingPriority.P2
    DomainPriority.P3 -> RingPriority.P3
    DomainPriority.P4 -> RingPriority.None
}

fun ChipIcon.vector(): ImageVector = when (this) {
    ChipIcon.Flag -> Icons.Outlined.Flag
    ChipIcon.Clock -> Icons.Outlined.Schedule
    ChipIcon.Calendar -> Icons.Outlined.CalendarMonth
    ChipIcon.Subtasks -> Icons.Outlined.Checklist
    ChipIcon.Repeat -> Icons.Outlined.Repeat
}

/** Linha de tarefa do kit alimentada por um [TaskRowUi]. */
@Composable
fun TaskRowItem(
    row: TaskRowUi,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TaskRow(
        title = row.title,
        checked = row.done,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        priority = row.priority.toRing(),
        onClick = onClick,
        meta = if (row.chips.isEmpty()) null else {
            { row.chips.forEach { PulsoChip(it.text, tone = it.tone, icon = it.icon?.vector()) } }
        },
    )
}

private enum class WhenShortcut { Today, Tomorrow, None }

/**
 * Editor de "quando": atalhos Hoje / Amanhã / Sem data e os painéis data | hora, que abrem
 * os seletores. Sem data não há hora.
 */
@Composable
fun WhenEditor(
    date: LocalDate?,
    time: LocalTime?,
    today: LocalDate,
    onChange: (LocalDate?, LocalTime?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    val shortcutLabels = mapOf(
        WhenShortcut.Today to stringResource(R.string.when_today),
        WhenShortcut.Tomorrow to stringResource(R.string.when_tomorrow),
        WhenShortcut.None to stringResource(R.string.when_no_date),
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickChips(
            options = WhenShortcut.entries,
            selected = when (date) {
                null -> WhenShortcut.None
                today -> WhenShortcut.Today
                today.plusDays(1) -> WhenShortcut.Tomorrow
                else -> null
            },
            onSelect = {
                when (it) {
                    WhenShortcut.Today -> onChange(today, time)
                    WhenShortcut.Tomorrow -> onChange(today.plusDays(1), time)
                    WhenShortcut.None -> onChange(null, null)
                }
            },
            label = { shortcutLabels.getValue(it) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldPanel(
                icon = Icons.Outlined.CalendarMonth,
                text = date?.let(PtBr::shortDate) ?: stringResource(R.string.when_pick_date),
                isPlaceholder = date == null,
                onClick = { showDate = true },
                modifier = Modifier.weight(1.25f),
            )
            FieldPanel(
                icon = Icons.Outlined.Schedule,
                text = time?.let(PtBr::time) ?: stringResource(R.string.when_no_time),
                isPlaceholder = time == null,
                enabled = date != null,
                onClick = { showTime = true },
                modifier = Modifier.weight(1f),
                trailing = if (time == null) null else {
                    {
                        Box(
                            Modifier.size(48.dp).clickable(role = Role.Button) { onChange(date, null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                stringResource(R.string.when_clear_time),
                                tint = Pulso.colors.text3,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
            )
        }
    }
    if (showDate) {
        PulsoDatePickerDialog(
            initial = date ?: today,
            onConfirm = { onChange(it, time); showDate = false },
            onDismiss = { showDate = false },
        )
    }
    if (showTime) {
        PulsoTimePickerDialog(
            title = stringResource(R.string.when_time_title),
            initial = time ?: LocalTime.of(9, 0),
            confirmLabel = ::useTimeLabel,
            onConfirm = { onChange(date, it); showTime = false },
            onDismiss = { showTime = false },
        )
    }
}

/**
 * Etiquetas: as existentes como chips de escolha (anel ember nas selecionadas) e um campo
 * para criar uma nova, que já entra selecionada.
 */
@Composable
fun TagPicker(
    allTags: List<Tag>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    // Etiquetas recém-criadas ainda não estão no banco: aparecem junto das existentes.
    val names = (allTags.map { it.name } + selected).distinct().sorted()
    val add = {
        val name = draft.trim().removePrefix("#").trim().lowercase()
        if (name.isNotEmpty() && name !in selected) onToggle(name)
        draft = ""
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (names.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                names.forEach { name -> SelectableChip("#$name", selected = name in selected, onClick = { onToggle(name) }) }
            }
        }
        PulsoTextField(
            value = draft,
            onValueChange = { draft = it },
            placeholder = stringResource(R.string.tags_new_placeholder),
            onImeAction = add,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Rótulo pequeno acima de um grupo de controles dentro de uma folha. */
@Composable
fun FieldLabel(text: String) {
    Text(text, style = Pulso.type.meta, color = Pulso.colors.text2)
}
