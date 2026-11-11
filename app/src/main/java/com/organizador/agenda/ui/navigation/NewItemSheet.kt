package com.organizador.agenda.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.ui.agenda.EventFormContent
import com.organizador.agenda.ui.agenda.EventFormViewModel
import com.organizador.agenda.ui.agenda.EventTarget
import com.organizador.agenda.ui.common.CALENDAR_PERMISSIONS
import com.organizador.agenda.ui.common.PulsoBottomSheet
import com.organizador.agenda.ui.common.rememberPermissionRequest
import com.organizador.agenda.ui.pulso.components.SegmentedPills
import com.organizador.agenda.ui.tasks.NewTaskForm
import java.time.LocalDateTime

enum class NewItemKind { Task, Event }

/** O que abrir na folha "Novo item". */
sealed interface SheetRequest {
    /** Criar: começa em [kind]; o seletor no topo troca entre tarefa e compromisso. */
    data class Create(
        val kind: NewItemKind,
        /** Data sugerida da tarefa, em dias a partir de hoje. */
        val taskDayOffset: Int = 0,
        /** Início sugerido do compromisso; null = próxima meia hora. */
        val eventStart: LocalDateTime? = null,
    ) : SheetRequest

    data class EditEvent(val target: EventTarget) : SheetRequest
}

/**
 * Folha "Novo item": tarefa ou compromisso. O seletor troca de visão, então é neutro (surface4).
 * Editar um compromisso usa a mesma folha, sem o seletor.
 */
@Composable
fun NewItemSheet(
    request: SheetRequest,
    onDismiss: () -> Unit,
    eventViewModel: EventFormViewModel = hiltViewModel(),
) {
    val create = request as? SheetRequest.Create
    var kind by remember(request) { mutableStateOf(create?.kind ?: NewItemKind.Event) }
    val eventState by eventViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(request) {
        eventViewModel.start((request as? SheetRequest.EditEvent)?.target, create?.eventStart)
    }
    val calendarPermission = rememberPermissionRequest(
        permissions = CALENDAR_PERMISSIONS,
        alreadyRequested = eventState.calendarPermissionRequested,
        onRequested = { eventViewModel.onCalendarPermissionRequested() },
        onResult = { eventViewModel.onCalendarPermissionResult() },
    )

    PulsoBottomSheet(onDismiss) {
        // Depois de gravar um compromisso, a confirmação ocupa a folha inteira.
        val confirming = kind == NewItemKind.Event && eventState.confirmation != null
        if (create != null && !confirming) {
            SegmentedPills(
                options = listOf(stringResource(R.string.new_item_task), stringResource(R.string.new_item_event)),
                selectedIndex = kind.ordinal,
                onSelect = { kind = NewItemKind.entries[it] },
            )
        }
        when (kind) {
            NewItemKind.Task -> NewTaskForm(dayOffset = create?.taskDayOffset ?: 0, onDone = onDismiss)
            NewItemKind.Event -> EventFormContent(
                state = eventState,
                calendarPermission = calendarPermission,
                onChange = { eventViewModel.update(it) },
                onSave = eventViewModel::save,
                onDone = onDismiss,
            )
        }
    }
}
