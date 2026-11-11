package com.organizador.agenda.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.Priority
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.PulsoTextField
import com.organizador.agenda.ui.common.isCompactHeight
import com.organizador.agenda.ui.pulso.components.LabelIcon
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.QuickChips
import com.organizador.agenda.ui.pulso.components.SectionLabel

/**
 * Formulário "Nova tarefa", conteúdo da folha Novo item (variante de tarefa do padrão Novo item): título, quando, prioridade,
 * lembrete, etiquetas e um botão que diz o resultado ("Criar tarefa · Amanhã, 09:00").
 *
 * @param dayOffset data sugerida, em dias a partir de hoje (0 = hoje, 1 = amanhã).
 */
@Composable
fun ColumnScope.NewTaskForm(
    dayOffset: Int,
    onDone: () -> Unit,
    viewModel: NewTaskViewModel = hiltViewModel(),
) {
    val today = remember { viewModel.today() }
    val allTags by viewModel.tags.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf(NewTaskDraft(date = today.plusDays(dayOffset.toLong()))) }
    val focus = remember { FocusRequester() }
    // Em tela baixa (paisagem) o teclado cobriria a folha inteira: o foco fica por conta do usuário.
    if (!isCompactHeight()) LaunchedEffect(Unit) { focus.requestFocus() }

    run {
        SectionLabel(stringResource(R.string.new_task_title), leading = { LabelIcon(Icons.Outlined.TaskAlt) })

        PulsoTextField(
            value = draft.title,
            onValueChange = { draft = draft.copy(title = it) },
            placeholder = stringResource(R.string.new_task_placeholder),
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(stringResource(R.string.field_when))
            WhenEditor(draft.date, draft.time, today, onChange = { d, t -> draft = draft.copy(date = d, time = t) })
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(stringResource(R.string.field_priority))
            QuickChips(Priority.entries, draft.priority, { draft = draft.copy(priority = it) }, { it.name })
        }

        // Lembrete só faz sentido com data.
        if (draft.date != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.field_reminder))
                val none = stringResource(R.string.reminder_none)
                QuickChips(
                    options = NewTaskViewModel.REMINDER_OPTIONS,
                    selected = draft.reminderMinutes,
                    onSelect = { draft = draft.copy(reminderMinutes = it) },
                    label = { m -> if (m == null) none else if (m == 0) "Na hora" else PtBr.duration(m) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FieldLabel(stringResource(R.string.field_tags))
            TagPicker(
                allTags = allTags,
                selected = draft.tags,
                onToggle = { name -> draft = draft.copy(tags = if (name in draft.tags) draft.tags - name else draft.tags + name) },
            )
        }

        val whenText = draft.date?.let { " · " + TaskUiMapper.ctaWhen(it, draft.time, today) }.orEmpty()
        PrimaryButton(
            text = stringResource(R.string.new_task_cta) + whenText,
            onClick = {
                viewModel.create(draft)
                onDone()
            },
            enabled = draft.isValid,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}
