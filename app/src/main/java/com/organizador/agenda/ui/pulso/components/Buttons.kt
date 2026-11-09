package com.organizador.agenda.ui.pulso.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import com.organizador.agenda.ui.pulso.PulsoShapes

@Composable
private fun pressScale(source: MutableInteractionSource): Float {
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, tween(PulsoMotion.QUICK), label = "press")
    return s
}

/**
 * Ember CTA (riff 02 "Send money"): vertical ember gradient, dark ink label, soft ember glow.
 * One per screen. Label states the outcome: "Agendar · Ter 10, 14:00", not "OK".
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = Pulso.colors
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .then(
                if (enabled) Modifier.shadow(14.dp, PulsoShapes.button, clip = false, ambientColor = c.ember, spotColor = c.ember)
                else Modifier
            )
            .clip(PulsoShapes.button)
            .background(Brush.verticalGradient(listOf(c.emberHi, c.ember)))
            .clickable(source, ripple(color = c.onEmber), enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 54.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = c.onEmber, modifier = Modifier.size(20.dp))
        Text(text, style = Pulso.type.button, color = c.onEmber, maxLines = 1)
    }
}

/** Neutral button (riff 02 "Done"): surface3 + hairline. Use for Cancel, Pausar, Concluir (dismiss). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = Pulso.colors
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.45f }
            .clip(PulsoShapes.button)
            .background(c.surface3)
            .border(1.dp, c.line, PulsoShapes.button)
            .clickable(source, ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = c.text, modifier = Modifier.size(19.dp))
        Text(text, style = Pulso.type.button, color = c.text, maxLines = 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
/** Ember round "create" button that sits beside the nav pill. Long-press can open Quick Capture. */
@Composable
fun EmberFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Add,
    size: androidx.compose.ui.unit.Dp = 58.dp,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
) {
    val c = Pulso.colors
    val source = remember { MutableInteractionSource() }
    val scale = pressScale(source)
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(16.dp, CircleShape, clip = false, ambientColor = c.ember, spotColor = c.ember)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(c.emberHi, c.ember)))
            .combinedClickable(
                interactionSource = source,
                indication = ripple(color = c.onEmber),
                role = Role.Button,
                onLongClickLabel = onLongClickLabel,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = c.onEmber, modifier = Modifier.size(size * 0.42f))
    }
}

/** Small circular icon button on a translucent surface (close ×, mic, overflow). 40dp visual, 48dp target. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pulso.colors
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(c.surface1.copy(alpha = 0.7f))
                .border(1.dp, c.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription, tint = c.text, modifier = Modifier.size(19.dp)) }
    }
}
