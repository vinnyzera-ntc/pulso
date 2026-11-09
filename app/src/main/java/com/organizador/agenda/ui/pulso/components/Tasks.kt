package com.organizador.agenda.ui.pulso.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import kotlinx.coroutines.launch

/** Priority is shown on the checkbox ring, so rows stay clean. */
enum class Priority { P1, P2, P3, None }

/**
 * Round checkbox. Ring color = priority (P1 flare, P2 ember, P3 volt, none lineStrong).
 * Checked = aurora fill + dark check, with a one-shot aurora halo ring (the "Halo" motion).
 * 22dp visual inside a 48dp touch target.
 */
@Composable
fun PriorityCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    priority: Priority = Priority.None,
    contentDescription: String? = null,
) {
    val c = Pulso.colors
    val reduced = Pulso.reducedMotion
    val scope = rememberCoroutineScope()
    val halo = remember { Animatable(1f) }
    val ring = when (priority) {
        Priority.P1 -> c.flare
        Priority.P2 -> c.ember
        Priority.P3 -> c.volt
        Priority.None -> c.lineStrong
    }
    val fill by animateColorAsState(if (checked) c.aurora else Color.Transparent, tween(PulsoMotion.BASE), label = "fill")
    val checkScale by animateFloatAsState(
        if (checked) 1f else 0f,
        if (reduced) tween(0) else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    Box(
        modifier
            .size(48.dp)
            .toggleable(value = checked, role = Role.Checkbox) { now ->
                if (now && !reduced) scope.launch { halo.snapTo(0f); halo.animateTo(1f, tween(PulsoMotion.HALO)) }
                onCheckedChange(now)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .drawBehind {
                    val t = halo.value
                    if (t < 1f) drawCircle(
                        color = c.aurora.copy(alpha = 0.55f * (1f - t)),
                        radius = size.minDimension / 2f * (1f + 0.9f * t),
                        style = Stroke(1.5.dp.toPx()),
                    )
                    drawCircle(fill)
                }
                .border(1.6.dp, if (checked) c.aurora else ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check, contentDescription,
                tint = c.onAurora,
                modifier = Modifier.size(14.dp).scale(checkScale),
            )
        }
    }
}

/**
 * Task row: checkbox · title · meta chips. Done = text3 + strikethrough.
 * Place rows in a Column separated by `HorizontalDivider(color = Pulso.colors.line, modifier = Modifier.padding(start = 35.dp))`.
 */

@Composable
fun TaskRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    priority: Priority = Priority.None,
    onClick: (() -> Unit)? = null,
    meta: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val c = Pulso.colors
    // The 48dp touch target would push the 22dp ring 13dp inside the screen gutter; pull the
    // checkbox back so the ring lines up with section labels. Row stays full width.
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        PriorityCheckbox(checked, onCheckedChange, Modifier.offset(x = (-13).dp), priority, contentDescription = title)
        Column(Modifier.weight(1f).offset(x = (-13).dp).padding(top = 12.dp, bottom = 10.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                title,
                style = Pulso.type.body,
                color = if (checked) c.text3 else c.text,
                textDecoration = if (checked) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            if (meta != null && !checked) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = meta)
            }
        }
    }
}

/** Segmented day progress (riff 02 nested panels + aurora): one segment per task, done = aurora. */
@Composable
fun ProgressSegments(total: Int, done: Int, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total.coerceAtLeast(1)) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .drawBehind { drawRoundRect(if (i < done) c.aurora else c.surface4, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height)) }
            )
        }
    }
}
