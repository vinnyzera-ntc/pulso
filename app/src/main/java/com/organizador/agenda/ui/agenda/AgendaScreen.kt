package com.organizador.agenda.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.organizador.agenda.R
import com.organizador.agenda.domain.model.EventDetails
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.common.CALENDAR_PERMISSIONS
import com.organizador.agenda.ui.common.CappedFontScale
import com.organizador.agenda.ui.common.CalendarDot
import com.organizador.agenda.ui.common.DestructiveSecondaryButton
import com.organizador.agenda.ui.common.PermissionPanel
import com.organizador.agenda.ui.common.PulsoBottomSheet
import com.organizador.agenda.ui.common.ScreenHeader
import com.organizador.agenda.ui.common.calendarPermissionCopy
import com.organizador.agenda.ui.common.rememberPermissionRequest
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.components.ChipTone
import com.organizador.agenda.ui.pulso.components.PulsoChip
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.components.SegmentedPills
import com.organizador.agenda.ui.pulso.components.WeekStrip
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.flow.first

private enum class PendingAction { Edit, Cancel }

@Composable
fun AgendaScreen(
    contentPadding: PaddingValues,
    onNewEvent: (LocalDateTime) -> Unit,
    onEditEvent: (EventTarget) -> Unit,
    viewModel: AgendaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    val permission = rememberPermissionRequest(
        permissions = CALENDAR_PERMISSIONS,
        alreadyRequested = state.permissionRequested,
        onRequested = { viewModel.onPermissionRequested() },
        onResult = { viewModel.onResume() },
    )
    var detail by remember { mutableStateOf<EventBlockUi?>(null) }
    var allDayDetail by remember { mutableStateOf<AllDayUi?>(null) }
    // Bloco recorrente arrastado: espera a escolha "Só este / Toda a série".
    var pendingMove by remember { mutableStateOf<Pair<EventBlockUi, Int>?>(null) }

    val onMove: (EventBlockUi, Int) -> Unit = { block, newStart ->
        if (block.recurring) pendingMove = block to newStart else viewModel.move(block, newStart, EditScope.Single)
    }

    // Paisagem no celular (pouca altura): dois painéis. À esquerda, cabeçalho, navegação e a faixa
    // da semana (ou a grade do mês); à direita, a linha do tempo (ou a lista do dia).
    val config = LocalConfiguration.current
    val twoPane = config.screenHeightDp < 480 && config.screenWidthDp > config.screenHeightDp
    val ready = state.loaded && state.hasAccess

    val header: @Composable () -> Unit = {
        ScreenHeader(title = state.title, eyebrow = state.eyebrow, wideActions = true) {
            SegmentedPills(
                options = listOf(
                    stringResource(R.string.agenda_view_day),
                    stringResource(R.string.agenda_view_week),
                    stringResource(R.string.agenda_view_month),
                ),
                selectedIndex = state.mode.ordinal,
                onSelect = { viewModel.setMode(AgendaMode.entries[it]) },
                modifier = Modifier.padding(end = 10.dp),
            )
        }
    }
    // Navegador + faixa da semana (visão Semana) + chips de dia inteiro (Dia e Semana).
    val controls: @Composable () -> Unit = {
        Navigator(state.navigatorLabel, showToday = state.selected != state.today, onStep = viewModel::step, onToday = viewModel::goToday)
        if (state.mode == AgendaMode.Week) {
            val onStep by rememberUpdatedState(viewModel::step)
            var dragged by remember { mutableFloatStateOf(0f) }
            // 7 colunas fixas: com fonte enorme os rótulos quebrariam; a faixa para em 1,3×.
            CappedFontScale {
                WeekStrip(
                    days = state.weekDays,
                    selected = state.selected,
                    today = state.today,
                    onSelect = viewModel::select,
                    eventCount = { state.eventCounts[it] ?: 0 },
                    locale = PtBr.locale,
                    modifier = Modifier
                        .padding(horizontal = PulsoSpace.gutter)
                        .padding(top = 4.dp)
                        // Deslizar a faixa troca de semana.
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragged = 0f },
                                onHorizontalDrag = { _, amount -> dragged += amount },
                                onDragEnd = { if (dragged > 80.dp.toPx()) onStep(-1) else if (dragged < -80.dp.toPx()) onStep(1) },
                            )
                        },
                )
            }
        }
        if (state.mode != AgendaMode.Month) AllDayRow(state.allDay, onClick = { allDayDetail = it })
    }
    val timeline: @Composable () -> Unit = {
        TimelineScroller(state, contentPadding) {
            AgendaTimeline(
                blocks = state.blocks,
                nowMinutes = state.nowMinutes,
                onEventClick = { detail = it },
                onEmptySlot = { minutes -> onNewEvent(viewModel.startAt(minutes)) },
                onMove = onMove,
                modifier = Modifier.padding(horizontal = PulsoSpace.gutter),
            )
        }
    }
    val permissionPanel: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = PulsoSpace.gutter), contentAlignment = Alignment.Center) {
            PermissionPanel(permission, Icons.Outlined.EventAvailable, calendarPermissionCopy())
        }
    }
    val bottomSpace = contentPadding.calculateBottomPadding() + 16.dp

    // Agenda: sem aurora (tela densa, utilitária).
    if (twoPane) {
        Row(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.width(352.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
                header()
                if (ready) {
                    controls()
                    if (state.mode == AgendaMode.Month) MonthGrid(state, viewModel::select, Modifier.padding(horizontal = PulsoSpace.gutter))
                }
                Spacer(Modifier.height(bottomSpace))
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    !state.loaded -> Unit
                    !state.hasAccess -> permissionPanel()
                    state.mode == AgendaMode.Month -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PulsoSpace.gutter)) {
                        MonthDayList(state, onEventClick = { detail = it }, onAllDayClick = { allDayDetail = it })
                        Spacer(Modifier.height(bottomSpace))
                    }
                    else -> timeline()
                }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            header()
            when {
                !state.loaded -> Unit
                !state.hasAccess -> permissionPanel()
                else -> {
                    controls()
                    if (state.mode == AgendaMode.Month) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PulsoSpace.gutter)) {
                            MonthGrid(state, viewModel::select)
                            MonthDayList(state, onEventClick = { detail = it }, onAllDayClick = { allDayDetail = it })
                            Spacer(Modifier.height(bottomSpace))
                        }
                    } else {
                        timeline()
                    }
                }
            }
        }
    }

    detail?.let { block ->
        EventDetailSheet(
            block = block,
            date = state.selected,
            loadDetails = { viewModel.details(block.eventId) },
            onEdit = { scope -> detail = null; onEditEvent(block.target(scope)) },
            onCancel = { scope -> detail = null; viewModel.cancel(block.target(scope)) },
            onDismiss = { detail = null },
        )
    }
    allDayDetail?.let { event ->
        PulsoBottomSheet({ allDayDetail = null }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalendarDot(event.color)
                Text(event.title, style = Pulso.type.title, color = Pulso.colors.text)
            }
            PulsoChip("${PtBr.shortDate(state.selected)} · ${stringResource(R.string.agenda_all_day)}", tone = ChipTone.Volt, icon = Icons.Outlined.Schedule)
            Text(stringResource(R.string.event_all_day_note), style = Pulso.type.bodyS, color = Pulso.colors.text2)
            SecondaryButton(stringResource(R.string.action_close), { allDayDetail = null }, Modifier.fillMaxWidth())
        }
    }
    pendingMove?.let { (block, newStart) ->
        PulsoBottomSheet({ pendingMove = null }) {
            ScopeChoice(stringResource(R.string.scope_move_body)) { scope ->
                pendingMove = null
                viewModel.move(block, newStart, scope)
            }
        }
    }
}

/** "‹ Ter, 10 nov ›" + atalho "Hoje" quando o dia mostrado não é hoje. */
@Composable
private fun Navigator(label: String, showToday: Boolean, onStep: (Int) -> Unit, onToday: () -> Unit) {
    val c = Pulso.colors
    Row(
        Modifier.fillMaxWidth().padding(start = 6.dp, end = PulsoSpace.gutter, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.agenda_prev)) { onStep(-1) }
        Text(label, style = Pulso.type.titleS, color = c.text)
        NavArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.agenda_next)) { onStep(1) }
        Spacer(Modifier.weight(1f))
        if (showToday) {
            Box(
                Modifier.heightIn(min = 48.dp).clip(PulsoShapes.chip).clickable(role = Role.Button, onClick = onToday).padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) { PulsoChip(stringResource(R.string.agenda_today)) }
        }
    }
}

@Composable
private fun NavArrow(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, description, tint = Pulso.colors.text2, modifier = Modifier.size(22.dp))
    }
}

/** Eventos de dia inteiro: uma fileira de chips neutros acima da linha do tempo. */
@Composable
private fun AllDayRow(events: List<AllDayUi>, onClick: (AllDayUi) -> Unit) {
    if (events.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = PulsoSpace.gutter),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        events.forEach { e ->
            Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { onClick(e) }, contentAlignment = Alignment.Center) {
                PulsoChip(e.title)
            }
        }
    }
}

/** Rola a linha do tempo; ao abrir ou trocar de dia, deixa o "agora" (ou o 1º evento) a ~1/3 do topo. */
@Composable
private fun TimelineScroller(state: AgendaUiState, contentPadding: PaddingValues, content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    val hourPx = with(LocalDensity.current) { TimelineHourHeight.toPx() }
    LaunchedEffect(state.selected, state.mode) {
        val anchor = state.nowMinutes ?: state.blocks.minOfOrNull { it.startMin } ?: (8 * 60)
        val viewport = snapshotFlow { scroll.viewportSize }.first { it > 0 }
        scroll.scrollTo((anchor / 60f * hourPx - viewport / 3f).toInt().coerceAtLeast(0))
    }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(top = 12.dp)) {
        content()
        Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 16.dp))
    }
}

// ---------- Mês ----------

/** Grade do mês: iniciais dos dias da semana + uma célula por dia. */
@Composable
private fun MonthGrid(state: AgendaUiState, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    val month = YearMonth.from(state.selected)
    CappedFontScale {
        Column(modifier) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                state.monthWeeks.firstOrNull()?.forEach { d ->
                    Text(
                        PtBr.weekdayShort(d.dayOfWeek).take(1).uppercase(PtBr.locale),
                        style = Pulso.type.eyebrow,
                        color = c.text3,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            state.monthWeeks.forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { d ->
                        DayCell(
                            date = d,
                            inMonth = YearMonth.from(d) == month,
                            selected = d == state.selected,
                            today = d == state.today,
                            events = state.eventCounts[d] ?: 0,
                            onClick = { onSelect(d) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** Compromissos do dia selecionado na visão Mês: chips de dia inteiro + blocos em lista. */
@Composable
private fun MonthDayList(state: AgendaUiState, onEventClick: (EventBlockUi) -> Unit, onAllDayClick: (AllDayUi) -> Unit) {
    val c = Pulso.colors
    SectionLabel(PtBr.shortDate(state.selected), Modifier.padding(top = 16.dp, bottom = 8.dp))
    if (state.blocks.isEmpty() && state.allDay.isEmpty()) {
        Text(stringResource(R.string.agenda_empty_day), style = Pulso.type.bodyS, color = c.text2, modifier = Modifier.padding(vertical = 8.dp))
    }
    if (state.allDay.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            state.allDay.forEach { e ->
                Box(Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { onAllDayClick(e) }, contentAlignment = Alignment.Center) {
                    PulsoChip(e.title)
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.blocks.forEach { block ->
            TimelineBlock(block, block.timeRange, dragging = false, onClick = { onEventClick(block) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp))
        }
    }
}

/** Célula do mês: 40dp num alvo de 48; selecionado = anel ember; hoje = número e ponto volt; pontos = eventos. */
@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    events: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pulso.colors
    val description = PtBr.shortDate(date) + if (events > 0) ", $events" else ""
    Box(
        modifier.height(50.dp).selectable(selected = selected, role = Role.Tab, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .size(40.dp)
                .clip(PulsoShapes.day)
                .background(if (selected) c.emberSoft else androidx.compose.ui.graphics.Color.Transparent)
                .then(if (selected) Modifier.border(1.5.dp, c.ember, PulsoShapes.day) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = Pulso.type.numeric.copy(fontSize = Pulso.type.bodyS.fontSize, fontWeight = if (today) FontWeight.SemiBold else FontWeight.Medium),
                color = when {
                    selected -> c.emberText
                    today -> c.voltText
                    inMonth -> c.text
                    else -> c.text3
                },
            )
            Box(Modifier.height(5.dp), contentAlignment = Alignment.Center) {
                if (today) {
                    Box(
                        Modifier.size(5.dp).drawBehind {
                            drawCircle(c.voltGlow.copy(alpha = 0.35f), radius = size.minDimension * 1.3f)
                            drawCircle(c.volt)
                        },
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(events.coerceAtMost(3)) { Box(Modifier.size(3.dp).clip(CircleShape).background(c.text3)) }
                    }
                }
            }
        }
    }
}

// ---------- Detalhe do compromisso ----------

/** "Só este" / "Toda a série" para eventos que se repetem. */
@Composable
private fun ScopeChoice(body: String, onChoose: (EditScope) -> Unit) {
    val c = Pulso.colors
    SectionLabel(stringResource(R.string.scope_title), leading = { Icon(Icons.Outlined.Repeat, null, tint = c.text2, modifier = Modifier.size(15.dp)) })
    Text(body, style = Pulso.type.bodyS, color = c.text2)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton(stringResource(R.string.scope_only_this), { onChoose(EditScope.Occurrence) }, Modifier.weight(1f))
        SecondaryButton(stringResource(R.string.scope_series), { onChoose(EditScope.Series) }, Modifier.weight(1f))
    }
}

@Composable
private fun EventDetailSheet(
    block: EventBlockUi,
    date: LocalDate,
    loadDetails: suspend () -> EventDetails?,
    onEdit: (EditScope) -> Unit,
    onCancel: (EditScope) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Pulso.colors
    val details by produceState<EventDetails?>(null, block.eventId) { value = loadDetails() }
    var pending by remember { mutableStateOf<PendingAction?>(null) }

    PulsoBottomSheet(onDismiss) {
        pending?.let { action ->
            ScopeChoice(stringResource(if (action == PendingAction.Edit) R.string.scope_edit_body else R.string.scope_cancel_body)) { scope ->
                if (action == PendingAction.Edit) onEdit(scope) else onCancel(scope)
            }
            return@PulsoBottomSheet
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CalendarDot(block.color)
            Text(block.title, style = Pulso.type.title, color = c.text)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Horário = papel de tempo (volt).
            PulsoChip("${PtBr.shortDate(date)} · ${block.timeRange}", tone = ChipTone.Volt, icon = Icons.Outlined.Schedule)
            block.location?.let { PulsoChip(it, icon = Icons.Outlined.Place) }
            if (block.recurring) PulsoChip(stringResource(R.string.event_repeats), icon = Icons.Outlined.Repeat)
            details?.reminderMinutes?.forEach { PulsoChip(PtBr.reminderBefore(it), tone = ChipTone.Volt, icon = Icons.Outlined.NotificationsNone) }
        }
        details?.let { d ->
            if (d.attendees.isNotEmpty()) {
                Text(
                    stringResource(R.string.event_with, d.attendees.joinToString(", ") { it.name }),
                    style = Pulso.type.bodyS,
                    color = c.text2,
                )
            }
            if (d.description.isNotBlank()) {
                Text(d.description.trim(), style = Pulso.type.bodyS, color = c.text2, maxLines = 8)
            }
        }
        if (block.canEdit) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                SecondaryButton(
                    stringResource(R.string.event_edit),
                    { if (block.recurring) pending = PendingAction.Edit else onEdit(EditScope.Single) },
                    Modifier.weight(1f),
                )
                DestructiveSecondaryButton(
                    stringResource(R.string.event_cancel),
                    { if (block.recurring) pending = PendingAction.Cancel else onCancel(EditScope.Single) },
                    Modifier.weight(2.2f),
                )
            }
        } else {
            Text(stringResource(R.string.event_read_only), style = Pulso.type.meta, color = c.text3)
        }
    }
}
