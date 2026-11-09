package com.organizador.agenda.ui.pulso.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoColors
import com.organizador.agenda.ui.pulso.PulsoShapes
import java.util.Locale

enum class SurfaceLevel { Card, Panel, Inset }

/**
 * Pulso surface: tonal fill + 1dp hairline. Depth comes from nesting levels
 * (Card ▸ Panel ▸ Inset), never from drop shadows.
 */
@Composable
fun PulsoCard(
    modifier: Modifier = Modifier,
    level: SurfaceLevel = SurfaceLevel.Card,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pulso.colors
    val (fill, shape) = when (level) {
        SurfaceLevel.Card -> c.surface1 to PulsoShapes.card
        SurfaceLevel.Panel -> c.surface2 to PulsoShapes.panel
        SurfaceLevel.Inset -> c.surface3 to PulsoShapes.control
    }
    Column(
        modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, c.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * Section label (riff 02 "QUICK SEND ⓘ … View all ›"): small icon + caps label, optional action link.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = Pulso.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).semantics { heading() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            leading?.invoke()
            Text(text.uppercase(Locale.getDefault()), style = Pulso.type.eyebrow, color = c.text2)
        }
        if (actionLabel != null) {
            Row(
                Modifier
                    .clip(PulsoShapes.chip)
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(actionLabel, style = Pulso.type.meta, color = c.text2)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.text2, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Small icon sized for SectionLabel's leading slot. */
@Composable
fun LabelIcon(icon: ImageVector) {
    Icon(icon, null, tint = Pulso.colors.text2, modifier = Modifier.size(15.dp))
}

/**
 * Aurora atmosphere (riff 03): a wide mint glow from the top edge, used only on hero screens
 * (Today, Capture, empty states, onboarding). Apply to the screen root, behind content.
 */
fun Modifier.aurora(colors: PulsoColors, intensity: Float = colors.auroraIntensity): Modifier = drawBehind {
    val center = Offset(size.width / 2f, -size.width * 0.18f)
    val radius = size.width * 0.85f
    val brush = Brush.radialGradient(
        0f to colors.mint.copy(alpha = intensity),
        0.45f to colors.mint.copy(alpha = intensity * 0.35f),
        1f to Color.Transparent,
        center = center,
        radius = radius,
    )
    scale(scaleX = 1.5f, scaleY = 1f, pivot = center) {
        drawCircle(brush = brush, radius = radius, center = center)
    }
}
