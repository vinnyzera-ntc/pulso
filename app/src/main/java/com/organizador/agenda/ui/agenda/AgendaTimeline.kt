package com.organizador.agenda.ui.agenda

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.organizador.agenda.domain.calendar.AgendaMath
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import com.organizador.agenda.ui.pulso.components.NowLine
import java.time.LocalTime

val TimelineHourHeight: Dp = 56.dp
private val Gutter = 46.dp
private val ColumnGap = 4.dp

/**
 * Linha do tempo de um dia (00:00–24:00): trilhos de hora em hairline, blocos de evento em
 * colunas quando se sobrepõem e a linha volt do "agora". Coloque dentro de um verticalScroll.
 *
 * - Toque num horário vazio: [onEmptySlot] com o minuto do dia, encaixado em 30 min.
 * - Segurar e arrastar um bloco: ele levanta (escala 1.02, borda ember) e encaixa de 15 em 15 min;
 *   ao soltar, [onMove] recebe o novo início.
 */
@Composable
fun AgendaTimeline(
    blocks: List<EventBlockUi>,
    nowMinutes: Int?,
    onEventClick: (EventBlockUi) -> Unit,
    onEmptySlot: (minutes: Int) -> Unit,
    onMove: (EventBlockUi, newStartMin: Int) -> Unit,
    modifier: Modifier = Modifier,
    hourHeight: Dp = TimelineHourHeight,
) {
    val c = Pulso.colors
    val density = LocalDensity.current
    val hourPx = with(density) { hourHeight.toPx() }
    fun y(minutes: Int): Dp = hourHeight * (minutes / 60f)

    var draggingKey by remember { mutableStateOf<Long?>(null) }
    var dragPx by remember { mutableFloatStateOf(0f) }
    val currentOnEmptySlot by rememberUpdatedState(onEmptySlot)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(hourHeight * 24)
            .pointerInput(hourPx) {
                detectTapGestures { pos ->
                    if (pos.x > Gutter.toPx()) {
                        val minutes = (pos.y / hourPx * 60).toInt()
                        currentOnEmptySlot((minutes / 30 * 30).coerceIn(0, AgendaMath.MINUTES_PER_DAY - 30))
                    }
                }
            }
            .drawBehind {
                // Trilhos das horas (hairlines).
                for (h in 0..24) {
                    val lineY = h * hourPx
                    drawLine(c.line, Offset(Gutter.toPx() - 6.dp.toPx(), lineY), Offset(size.width, lineY), 1.dp.toPx())
                }
            },
    ) {
        val lane = maxWidth - Gutter - 4.dp
        for (h in 1..23) {
            // O rótulo da hora some quando o "agora" está em cima dele (os dois se sobreporiam).
            if (nowMinutes != null && kotlin.math.abs(nowMinutes - h * 60) < 25) continue
            Text(
                "%02d:00".format(h),
                style = Pulso.type.numeric.copy(fontSize = Pulso.type.eyebrow.fontSize),
                color = c.text3,
                modifier = Modifier.offset(y = hourHeight * h - 8.dp),
            )
        }

        blocks.forEach { block ->
            val dragging = draggingKey == block.key
            val duration = block.endMin - block.startMin
            val shownStart = if (dragging) {
                AgendaMath.dragTarget(block.startMin, duration, (dragPx / hourPx * 60).toInt())
            } else block.startMin
            val top = y(shownStart)
            val height = (y(block.endMin) - y(block.startMin)).coerceAtLeast(26.dp)
            val colWidth = lane / block.columns
            val lift by animateFloatAsState(if (dragging) 1.02f else 1f, tween(if (Pulso.reducedMotion) 0 else PulsoMotion.QUICK), label = "lift")
            val haptics = LocalHapticFeedback.current
            val currentBlock by rememberUpdatedState(block)
            val currentOnMove by rememberUpdatedState(onMove)

            TimelineBlock(
                block = block,
                // Enquanto arrasta, o horário do bloco mostra para onde ele vai.
                timeText = if (dragging) rangeAt(shownStart, duration) else block.timeRange,
                dragging = dragging,
                compact = height < 48.dp,
                onClick = { onEventClick(block) },
                modifier = Modifier
                    .zIndex(if (dragging) 1f else 0f)
                    .offset(x = Gutter + colWidth * block.column, y = top + 2.dp)
                    .width(colWidth - ColumnGap)
                    .height(height - 4.dp)
                    .graphicsLayer { scaleX = lift; scaleY = lift }
                    .then(
                        if (!block.canEdit) Modifier else Modifier.pointerInput(block.key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dragPx = 0f
                                    draggingKey = currentBlock.key
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragPx += amount.y
                                },
                                onDragEnd = {
                                    val b = currentBlock
                                    val target = AgendaMath.dragTarget(b.startMin, b.endMin - b.startMin, (dragPx / hourPx * 60).toInt())
                                    draggingKey = null
                                    if (target != b.startMin) currentOnMove(b, target)
                                },
                                onDragCancel = { draggingKey = null },
                            )
                        },
                    ),
            )
        }

        if (nowMinutes != null) {
            NowLine(
                LocalTime.of(nowMinutes / 60, nowMinutes % 60),
                Modifier.offset(y = y(nowMinutes) - 7.dp).fillMaxWidth().zIndex(2f),
                labelWidth = Gutter,
            )
        }
    }
}

private fun rangeAt(startMin: Int, duration: Int): String {
    fun t(m: Int) = PtBr.time(LocalTime.of((m / 60) % 24, m % 60))
    return "${t(startMin)} – ${t(startMin + duration)}"
}

/**
 * Bloco de evento (anatomia do EventBlock do kit): surface2 + hairline, raio 14, quadrado de 7dp
 * com a cor da agenda antes do título. Ao vivo: borda volt 60% + halo voltSoft. Arrastando: borda ember.
 */
@Composable
fun TimelineBlock(
    block: EventBlockUi,
    timeText: String,
    dragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Bloco baixo (evento curto): uma linha só, com o título e o horário de início. */
    compact: Boolean = false,
) {
    val c = Pulso.colors
    val shape = RoundedCornerShape(if (compact) 10.dp else 14.dp)
    val border = when {
        dragging -> c.ember
        block.live -> c.volt.copy(alpha = 0.6f)
        else -> c.line
    }
    val description = "${block.title}, ${block.timeRange}" + (block.location?.let { ", $it" } ?: "")
    Column(
        modifier
            .then(
                if (block.live && !dragging) Modifier.drawBehind {
                    drawRoundRect(
                        c.voltSoft,
                        topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                        size = Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                        cornerRadius = CornerRadius(17.dp.toPx()),
                    )
                } else Modifier,
            )
            .clip(shape)
            .background(c.surface2)
            .border(if (dragging) 1.5.dp else 1.dp, border, shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = if (compact) 0.dp else 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(2.dp)).background(Color(block.color)))
            Text(
                block.title,
                style = Pulso.type.meta.copy(fontWeight = FontWeight.SemiBold, fontSize = Pulso.type.bodyS.fontSize),
                color = c.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (compact) {
                Text(timeText.substringBefore(" –"), style = Pulso.type.numeric, color = if (dragging) c.emberText else c.text2, maxLines = 1)
            }
        }
        if (!compact) Text(
            listOfNotNull(timeText, block.location).joinToString(" · "),
            style = Pulso.type.numeric,
            color = if (dragging) c.emberText else c.text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
