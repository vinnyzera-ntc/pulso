package com.organizador.agenda.ui.pulso.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion

/**
 * Focus orbit (riff 01 Electro Wave): hairline tracks run in from both screen edges and curve into
 * a central timer pill; every 2.4s volt light pulses travel along them and "charge" the pill.
 * Fill the screen with it (background = Pulso.colors.deep); put labels/buttons on top.
 *
 * @param centerFraction vertical position of the pill (0..1 of the box height).
 */
@Composable
fun FocusOrbit(
    timeText: String,
    modifier: Modifier = Modifier,
    running: Boolean = true,
    centerFraction: Float = 0.42f,
) {
    val c = Pulso.colors
    val reduced = Pulso.reducedMotion
    val t by rememberInfiniteTransition(label = "orbit").animateFloat(
        0f, 1f, infiniteRepeatable(tween(PulsoMotion.PULSE, easing = PulsoMotion.Pulse), RepeatMode.Restart), label = "p",
    )
    val animate = running && !reduced
    BoxWithConstraints(modifier.fillMaxSize()) {
        val pillW = 184.dp
        val pillH = 72.dp
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cy = h * centerFraction
            val px = (w - pillW.toPx()) / 2f
            val ys = floatArrayOf(0.11f, 0.19f, 0.27f, 0.56f, 0.65f, 0.73f, 0.81f).map { it * h }
            val measure = PathMeasure()
            val seg = Path()
            ys.forEachIndexed { i, y ->
                val dy = (if (y < cy) -1f else 1f) * (10f + (i % 3) * 7f) * density
                val left = Path().apply {
                    moveTo(-4f, y); lineTo(18f * density, y)
                    cubicTo(px - 14f * density, y, px - 26f * density, cy + dy, px, cy + dy * 0.25f)
                }
                val right = Path().apply {
                    moveTo(w + 4f, y); lineTo(w - 18f * density, y)
                    cubicTo(w - px + 14f * density, y, w - px + 26f * density, cy + dy, w - px, cy + dy * 0.25f)
                }
                listOf(left, right).forEachIndexed { side, path ->
                    drawPath(path, c.lineStrong.copy(alpha = 0.8f), style = Stroke(1.2.dp.toPx()))
                    if (animate) {
                        val phase = ((t + i * 0.13f + side * 0.07f) % 1f)
                        measure.setPath(path, false)
                        val len = measure.length
                        val segLen = len * 0.09f
                        val end = len * phase
                        val start = (end - segLen).coerceAtLeast(0f)
                        val alpha = when { phase < 0.1f -> phase / 0.1f; phase > 0.9f -> (1f - phase) / 0.1f; else -> 1f }
                        seg.reset()
                        if (measure.getSegment(start, end, seg, true)) {
                            drawPath(seg, c.voltGlow.copy(alpha = 0.35f * alpha), style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
                            drawPath(seg, c.volt.copy(alpha = alpha), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                }
            }
        }
        val charge = if (animate) ((t - 0.70f) / 0.16f).coerceIn(0f, 1f) * (1f - ((t - 0.86f) / 0.14f).coerceIn(0f, 1f)) else 0f
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = maxHeight * centerFraction - pillH / 2)
                .size(pillW, pillH)
                .drawBehind {
                    if (charge > 0f) drawRoundRect(
                        c.voltGlow.copy(alpha = 0.45f * charge),
                        topLeft = androidx.compose.ui.geometry.Offset(-8.dp.toPx(), -8.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width + 16.dp.toPx(), size.height + 16.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height),
                    )
                }
                .clip(CircleShape)
                .background(c.surface2)
                .border(1.dp, if (charge > 0.2f) c.volt else c.lineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(timeText, style = Pulso.type.timer, color = c.text) }
    }
}
