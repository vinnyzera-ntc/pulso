package com.organizador.agenda.ui.tasks

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.domain.task.TaskFilter
import com.organizador.agenda.domain.task.TaskSection
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.common.PulsoTextField
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.common.SelectableChip
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.aurora

@Composable
fun TasksScreen(
    contentPadding: PaddingValues,
    onOpenTask: (Long) -> Unit,
    onNewTask: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = Pulso.colors
    // A lista não tem aurora; só o estado vazio (padrão "Empty" da skill).
    val showAurora = state.loaded && state.isEmpty
    Column(
        Modifier
            .fillMaxSize()
            .then(if (showAurora) Modifier.aurora(c) else Modifier)
            .statusBarsPadding(),
    ) {
        ScreenHeader(title = stringResource(R.string.nav_tasks)) {
            if (state.searching) {
                RoundIconButton(Icons.Outlined.Close, stringResource(R.string.tasks_close_search), { viewModel.setSearching(false) })
            } else {
                RoundIconButton(Icons.Outlined.Search, stringResource(R.string.action_search), { viewModel.setSearching(true) })
            }
        }
        if (!state.loaded) return@Column

        if (state.searching) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { focus.requestFocus() }
            PulsoTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = stringResource(R.string.tasks_search_placeholder),
                modifier = Modifier.padding(horizontal = PulsoSpace.gutter).padding(top = 12.dp).focusRequester(focus),
            )
        }
        FilterRow(state, viewModel::setFilter)

        if (state.isEmpty) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(contentPadding), contentAlignment = Alignment.Center) {
                val (title, body) = when {
                    state.noTasksAtAll -> R.string.tasks_empty_title to R.string.tasks_empty_body
                    state.query.isNotBlank() -> R.string.tasks_no_results_title to R.string.tasks_no_results_body
                    else -> R.string.tasks_filter_empty_title to R.string.tasks_filter_empty_body
                }
                EmptyState(
                    title = stringResource(title),
                    message = stringResource(body),
                    action = {
                        PrimaryButton(
                            stringResource(if (state.noTasksAtAll) R.string.tasks_create_first else R.string.tasks_create),
                            onNewTask,
                            Modifier.fillMaxWidth(),
                        )
                    },
                )
            }
            return@Column
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = PulsoSpace.gutter, end = PulsoSpace.gutter, top = 6.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            state.sections.forEach { group ->
                item(key = "label-${group.section}") {
                    SectionLabel(sectionTitle(group.section), Modifier.animateItem().padding(top = 16.dp, bottom = 2.dp))
                }
                itemsIndexed(group.rows, key = { _, t -> t.id }) { i, task ->
                    Column(Modifier.animateItem()) {
                        TaskRowItem(task, onCheckedChange = { viewModel.setDone(task.id, it) }, onClick = { onOpenTask(task.id) })
                        if (i < group.rows.lastIndex) HorizontalDivider(color = c.line, modifier = Modifier.padding(start = 35.dp))
                    }
                }
            }
            if (state.completed.isNotEmpty()) {
                item(key = "completed-header") {
                    CompletedHeader(state.completed.size, state.completedExpanded, viewModel::toggleCompleted, Modifier.animateItem())
                }
                if (state.completedExpanded) {
                    itemsIndexed(state.completed, key = { _, t -> t.id }) { i, task ->
                        Column(Modifier.animateItem()) {
                            TaskRowItem(task, onCheckedChange = { viewModel.setDone(task.id, it) }, onClick = { onOpenTask(task.id) })
                            if (i < state.completed.lastIndex) HorizontalDivider(color = c.line, modifier = Modifier.padding(start = 35.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Filtros: escolha de valor, então o selecionado leva o anel ember. Contagem de atrasadas em flare. */
@Composable
private fun FilterRow(state: TasksUiState, onSelect: (TaskFilter) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = PulsoSpace.gutter)
            .padding(top = 14.dp, bottom = 4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val f = state.filter
        SelectableChip(stringResource(R.string.filter_all), f == TaskFilter.All, { onSelect(TaskFilter.All) })
        SelectableChip(stringResource(R.string.filter_today), f == TaskFilter.Today, { onSelect(TaskFilter.Today) })
        SelectableChip(stringResource(R.string.filter_next7), f == TaskFilter.Next7Days, { onSelect(TaskFilter.Next7Days) })
        SelectableChip(
            stringResource(R.string.filter_overdue), f == TaskFilter.Overdue, { onSelect(TaskFilter.Overdue) },
            trailingText = state.overdueCount.takeIf { it > 0 }?.toString(),
        )
        SelectableChip(stringResource(R.string.filter_no_date), f == TaskFilter.NoDate, { onSelect(TaskFilter.NoDate) })
        state.tags.forEach { tag ->
            val tf = TaskFilter.ByTag(tag.id)
            SelectableChip("#${tag.name}", f == tf, { onSelect(if (f == tf) TaskFilter.All else tf) })
        }
    }
}

@Composable
private fun sectionTitle(section: TaskSection): String = stringResource(
    when (section) {
        TaskSection.Overdue -> R.string.section_overdue
        TaskSection.Today -> R.string.section_today
        TaskSection.Tomorrow -> R.string.section_tomorrow
        TaskSection.Next7Days -> R.string.section_next7
        TaskSection.Later -> R.string.section_later
        TaskSection.NoDate -> R.string.section_no_date
    },
)

/** "Concluídas (12)": recolhidas por padrão; a seta gira ao abrir. */
@Composable
private fun CompletedHeader(count: Int, expanded: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    val reduced = Pulso.reducedMotion
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, tween(if (reduced) 0 else PulsoMotion.BASE), label = "seta")
    val stateText = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics { heading(); stateDescription = stateText },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.tasks_completed_count, count).uppercase(),
            style = Pulso.type.eyebrow,
            color = c.text2,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = c.text2, modifier = Modifier.size(20.dp).rotate(rotation))
    }
}
