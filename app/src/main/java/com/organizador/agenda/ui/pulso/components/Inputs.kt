package com.organizador.agenda.ui.pulso.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion

/**
 * Big number entry (riff 02 "$5,763.05"): display-size value + unit, ember caret.
 * Use for duration, estimate, count, amount.
 */
@Composable
fun BigNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
) {
    val c = Pulso.colors
    BasicTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { it.isDigit() || it == ',' || it == '.' }) },
        modifier = modifier,
        singleLine = true,
        textStyle = Pulso.type.displayL.copy(color = c.text),
        cursorBrush = SolidColor(c.ember),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box {
                    if (value.isEmpty()) Text(placeholder, style = Pulso.type.displayL, color = c.text3)
                    inner()
                }
                Text(unit, style = Pulso.type.title.copy(fontWeight = FontWeight.Normal), color = c.text2, modifier = Modifier.padding(bottom = 8.dp))
            }
        },
    )
}

/**
 * Quick Capture field (riff 03): hairline box with a mint edge, "+" glyph, large display text.
 * Ranges returned by [recognized] (dates, #tags, @people your parser found) get the drifting
 * mint→aurora→volt highlight; the caret uses the same gradient. Reserve this treatment for
 * smart capture — it tells the user "the app understood this part".
 */
@Composable
fun CaptureField(
    value: String,
    onValueChange: (String) -> Unit,
    recognized: List<IntRange>,
    modifier: Modifier = Modifier,
    placeholder: String = "Ex.: Ligar para o contador amanhã às 9h #pessoal",
    footer: @Composable () -> Unit = {},
) {
    val c = Pulso.colors
    val reduced = Pulso.reducedMotion
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val drift by rememberInfiniteTransition(label = "capture").animateFloat(
        0f, 1f, infiniteRepeatable(tween(PulsoMotion.HIGHLIGHT, easing = LinearEasing), RepeatMode.Restart), label = "drift",
    )
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface1.copy(alpha = 0.72f))
            .border(1.dp, c.mint.copy(alpha = if (c.isDark) 0.28f else 0.55f), shape)
            .padding(start = 14.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Add, null, tint = c.text2, modifier = Modifier.padding(top = 4.dp).size(22.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, style = Pulso.type.title.copy(fontWeight = FontWeight.Normal), color = c.text3)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = Pulso.type.title.copy(fontWeight = FontWeight.Normal, color = c.text, lineHeight = Pulso.type.title.lineHeight * 1.12f),
                    cursorBrush = Brush.verticalGradient(listOf(c.aurora, c.voltGlow)),
                    onTextLayout = { layout = it },
                    modifier = Modifier.fillMaxWidth().drawBehind {
                        val l = layout ?: return@drawBehind
                        val len = l.layoutInput.text.length
                        val period = 240.dp.toPx()
                        val x0 = if (reduced) 0f else drift * period * 2f
                        val alpha = if (c.isDark) 0.48f else 0.38f
                        val brush = Brush.linearGradient(
                            c.captureGradient.map { it.copy(alpha = alpha) },
                            start = Offset(x0, 0f), end = Offset(x0 + period, 0f), tileMode = TileMode.Mirror,
                        )
                        recognized.forEach { r ->
                            val s = r.first.coerceIn(0, len)
                            val e = (r.last + 1).coerceIn(0, len)
                            if (e > s) drawPath(l.getPathForRange(s, e), brush)
                        }
                    },
                )
            }
        }
        footer()
    }
}
