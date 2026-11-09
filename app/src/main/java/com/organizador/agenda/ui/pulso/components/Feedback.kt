package com.organizador.agenda.ui.pulso.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import kotlinx.coroutines.launch

/** Live indicator: volt dot with an expanding ring. Only for things happening right now. */
@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    val c = Pulso.colors
    val reduced = Pulso.reducedMotion
    val t by rememberInfiniteTransition(label = "live").animateFloat(
        0f, 1f, infiniteRepeatable(tween(PulsoMotion.LIVE), RepeatMode.Restart), label = "ring",
    )
    Canvas(modifier.size(18.dp)) {
        val r = 4.dp.toPx()
        if (!reduced) drawCircle(c.voltGlow.copy(alpha = 0.6f * (1f - t)), radius = r + 6.dp.toPx() * t)
        drawCircle(c.volt, r)
    }
}

/**
 * "Agora" card: current event with remaining time in volt and a volt progress hairline along the
 * bottom edge (riff 01 track). progress = elapsed fraction 0..1.
 */
@Composable
fun NowEventCard(
    timeRange: String,
    remaining: String,
    title: String,
    progress: Float,
    modifier: Modifier = Modifier,
    location: String? = null,
    attendees: List<Person> = emptyList(),
    onClick: (() -> Unit)? = null,
) {
    val c = Pulso.colors
    PulsoCard(
        modifier.drawBehind {
            val y = size.height - 1.dp.toPx()
            val x = size.width * progress.coerceIn(0f, 1f)
            drawLine(c.line, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
            drawLine(c.volt, Offset(0f, y), Offset(x, y), 2.dp.toPx())
        },
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(timeRange, style = Pulso.type.numeric, color = c.text2)
            Text(remaining, style = Pulso.type.numeric.copy(fontWeight = FontWeight.SemiBold), color = c.voltText)
        }
        Text(title, style = Pulso.type.titleS, color = c.text)
        if (location != null || attendees.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (location != null) {
                    Icon(Icons.Outlined.Place, null, tint = c.text2, modifier = Modifier.size(15.dp))
                    Text(location, style = Pulso.type.meta, color = c.text2, modifier = Modifier.padding(start = 5.dp).weight(1f))
                } else Box(Modifier.weight(1f))
                if (attendees.isNotEmpty()) AvatarStack(attendees)
            }
        }
    }
}

/**
 * Success moment (riff 02 "Transfer successful"): aurora disc pops in with a spring, one halo
 * ring expands and fades, then a soft glow breathes. Pair with: aurora caption, the result as a
 * displayM value, a meta line, and a neutral "Concluir" button.
 */
@Composable
fun SuccessHalo(modifier: Modifier = Modifier) {
    val c = Pulso.colors
    val reduced = Pulso.reducedMotion
    val pop = remember { Animatable(if (reduced) 1f else 0.6f) }
    val ring = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            launch { pop.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
            ring.animateTo(1f, tween(PulsoMotion.HALO * 2))
        }
    }
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        0.85f, 1.05f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "b",
    )
    Box(modifier.size(150.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(150.dp)) {
            val base = size.minDimension / 2f
            val b = if (reduced) 1f else breathe
            drawCircle(Brush.radialGradient(listOf(c.auroraSoft, c.auroraSoft.copy(alpha = 0f)), radius = base * b), radius = base * b)
            if (ring.value < 1f) drawCircle(
                c.aurora.copy(alpha = 0.6f * (1f - ring.value)), radius = base * (0.62f + 0.38f * ring.value), style = Stroke(1.dp.toPx()),
            )
        }
        Box(
            Modifier.size(92.dp).scale(pop.value).drawBehind {
                drawCircle(c.aurora.copy(alpha = 0.35f), radius = size.minDimension / 2f + 10.dp.toPx())
                drawCircle(Brush.radialGradient(listOf(c.auroraHi, c.aurora), center = Offset(size.width / 2f, size.height * 0.3f), radius = size.minDimension * 0.75f))
            },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Check, null, tint = c.onAurora, modifier = Modifier.size(38.dp)) }
    }
}

/** Confirmation layout used after create/schedule/send. */
@Composable
fun ConfirmationContent(
    caption: String,
    value: String,
    meta: String,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit = {},
) {
    val c = Pulso.colors
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SuccessHalo(Modifier.padding(bottom = 14.dp))
        Text(caption, style = Pulso.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.auroraText)
        Text(value, style = Pulso.type.displayM, color = c.text)
        Text(meta, style = Pulso.type.bodyS, color = c.text2)
        Box(Modifier.height(20.dp))
        extra()
    }
}
