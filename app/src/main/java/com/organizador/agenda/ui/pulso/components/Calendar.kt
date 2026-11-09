package com.organizador.agenda.ui.pulso.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import com.organizador.agenda.ui.pulso.PulsoShapes
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

/**
 * Week strip. Selected day = ember ring (riff 02 avatar selection); today = volt dot with glow;
 * other days show up to 3 text3 dots for event count.
 */
@Composable
fun WeekStrip(
    days: List<LocalDate>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    eventCount: (LocalDate) -> Int = { 0 },
    locale: Locale = Locale.getDefault(),
) {
    val c = Pulso.colors
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        days.forEach { d ->
            val sel = d == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(PulsoShapes.day)
                    .background(if (sel) c.emberSoft else c.surface1)
                    .border(if (sel) 1.5.dp else 1.dp, if (sel) c.ember else c.line, PulsoShapes.day)
                    .selectable(selected = sel, role = Role.Tab) { onSelect(d) }
                    .padding(top = 7.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    d.dayOfWeek.getDisplayName(JTextStyle.SHORT, locale).trimEnd('.').uppercase(locale),
                    style = Pulso.type.eyebrow.copy(letterSpacing = Pulso.type.eyebrow.letterSpacing * 0.5f),
                    color = if (sel) c.emberText else c.text3,
                )
                Text(d.dayOfMonth.toString(), style = Pulso.type.titleS.copy(fontFamily = Pulso.type.numeric.fontFamily, fontFeatureSettings = "tnum"), color = if (sel) c.emberText else c.text)
                Box(Modifier.height(5.dp), contentAlignment = Alignment.Center) {
                    if (d == today) {
                        Box(Modifier.size(5.dp).drawBehind {
                            drawCircle(c.voltGlow.copy(alpha = 0.35f), radius = size.minDimension * 1.3f)
                            drawCircle(c.volt)
                        })
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(eventCount(d).coerceAtMost(3)) { Box(Modifier.size(3.dp).clip(CircleShape).background(c.text3)) }
                        }
                    }
                }
            }
        }
    }
}

enum class EventTone { Volt, Aurora, Ember, Neutral }

data class TimelineEvent(
    val id: String,
    val title: String,
    val subtitle: String?,
    val start: LocalTime,
    val end: LocalTime,
    val tone: EventTone = EventTone.Volt,
    /** Ember-tinted block: time the user reserved for a task (focus block). */
    val isTaskBlock: Boolean = false,
)

/**
 * Day timeline: hairline hour tracks (riff 01), events as tonal blocks, and the volt NowLine.
 * Height = (endHour - startHour) * hourHeight. Wrap in verticalScroll() at the call site.
 */
@Composable
fun DayTimeline(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier,
    startHour: Int = 7,
    endHour: Int = 22,
    hourHeight: Dp = 56.dp,
    now: LocalTime? = null,
    onEventClick: (TimelineEvent) -> Unit = {},
) {
    val c = Pulso.colors
    val gutter = 46.dp
    fun y(t: LocalTime): Dp = hourHeight * ((t.hour - startHour) + t.minute / 60f)
    Box(modifier.fillMaxWidth().height(hourHeight * (endHour - startHour))) {
        // hour tracks
        for (h in startHour until endHour) {
            Box(
                Modifier
                    .offset(y = hourHeight * (h - startHour))
                    .fillMaxWidth()
                    .height(hourHeight)
                    .drawBehind { drawLine(c.line, Offset(gutter.toPx() - 6.dp.toPx(), 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            ) {
                Text(
                    "%02d:00".format(h), style = Pulso.type.numeric.copy(fontSize = Pulso.type.eyebrow.fontSize), color = c.text3,
                    modifier = Modifier.offset(y = (-8).dp),
                )
            }
        }
        // events
        events.forEach { e ->
            val top = y(e.start)
            val h = (y(e.end) - top).coerceAtLeast(26.dp)
            val live = now != null && !now.isBefore(e.start) && now.isBefore(e.end)
            EventBlock(
                e, live = live, onClick = { onEventClick(e) },
                modifier = Modifier.padding(start = gutter, end = 4.dp).offset(y = top + 2.dp).height(h - 4.dp).fillMaxWidth(),
            )
        }
        // now line
        if (now != null && now.hour in startHour until endHour) {
            NowLine(now, Modifier.offset(y = y(now) - 7.dp).fillMaxWidth(), labelWidth = gutter)
        }
    }
}

@Composable
fun EventBlock(event: TimelineEvent, live: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    val shape = RoundedCornerShape(14.dp)
    val dot = when (event.tone) { EventTone.Volt -> c.volt; EventTone.Aurora -> c.aurora; EventTone.Ember -> c.ember; EventTone.Neutral -> c.text3 }
    val fill = if (event.isTaskBlock) c.emberSoft else c.surface2
    val border = when {
        live -> c.volt.copy(alpha = 0.6f)
        event.isTaskBlock -> c.ember.copy(alpha = 0.35f)
        else -> c.line
    }
    Column(
        modifier
            .then(if (live) Modifier.drawBehind {
                drawRoundRect(c.voltSoft, topLeft = Offset(-3.dp.toPx(), -3.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx()))
            } else Modifier)
            .clip(shape)
            .background(fill)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(2.dp)).background(dot))
            Text(event.title, style = Pulso.type.meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = Pulso.type.bodyS.fontSize), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        val sub = listOfNotNull("${event.start.format(fmt)} – ${event.end.format(fmt)}", event.subtitle).joinToString(" · ")
        Text(sub, style = Pulso.type.numeric, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The "now" line (riff 01 pulse): volt hairline with glowing dot and a light sweep that travels
 * along it every 2.4s. Static when reduced motion is on.
 */
@Composable
fun NowLine(now: LocalTime, modifier: Modifier = Modifier, labelWidth: Dp = 46.dp) {
    val c = Pulso.colors
    val reduced = Pulso.reducedMotion
    val t by rememberInfiniteTransition(label = "now").animateFloat(
        0f, 1f, infiniteRepeatable(tween(PulsoMotion.PULSE, easing = PulsoMotion.Pulse), RepeatMode.Restart), label = "sweep",
    )
    Row(modifier.height(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            now.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = Pulso.type.numeric.copy(fontSize = Pulso.type.eyebrow.fontSize, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = c.voltText,
            modifier = Modifier.width(labelWidth - 12.dp),
        )
        Canvas(Modifier.weight(1f).height(14.dp)) {
            val cy = size.height / 2f
            val r = 5.dp.toPx()
            drawLine(c.voltGlow.copy(alpha = 0.30f), Offset(r, cy), Offset(size.width, cy), 6.dp.toPx())
            drawLine(c.volt, Offset(r, cy), Offset(size.width, cy), 2.dp.toPx())
            drawCircle(c.voltGlow.copy(alpha = 0.35f), r * 1.9f, Offset(r, cy))
            drawCircle(c.volt, r, Offset(r, cy))
            if (!reduced) {
                val w = 60.dp.toPx()
                val x = -w + (size.width + w) * t
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.85f), Color.Transparent), startX = x, endX = x + w),
                    Offset(x.coerceAtLeast(r), cy), Offset((x + w).coerceAtMost(size.width), cy), 2.dp.toPx(),
                )
            }
        }
    }
}

