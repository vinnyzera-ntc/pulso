package com.organizador.agenda.ui.pulso.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoMotion
import com.organizador.agenda.ui.pulso.glass
import com.organizador.agenda.ui.pulso.glassVeil

data class NavItem(val label: String, val icon: ImageVector)

/**
 * Floating nav pill (riff 01 pill) + ember create button.
 * Selected tab expands to icon + label on surface4; others are icon-only in text3.
 * Put it in Scaffold(bottomBar = …) with containerColor = Pulso.colors.bg, or overlay it at the bottom.
 */
@Composable
fun BottomNavPill(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onCreate: (() -> Unit)? = null,
    createLabel: String = "Criar",
    onCreateLongPress: (() -> Unit)? = null,
    createLongPressLabel: String? = null,
) {
    val c = Pulso.colors
    // Em telas largas (paisagem) o pill não estica: fica com no máximo 460dp, centralizado.
    Box(modifier.fillMaxWidth().navigationBarsPadding(), contentAlignment = Alignment.Center) {
    Row(
        Modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(58.dp)
                // Liquid Glass (superfície flutuante); sem suporte, o pill opaco de antes.
                .glass(CircleShape, veil = c.glassVeil, fallback = c.surface2.copy(alpha = 0.94f))
                .clip(CircleShape)
                .padding(6.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, item ->
                val on = i == selectedIndex
                Row(
                    Modifier
                        .height(46.dp)
                        .clip(CircleShape)
                        .background(if (on) c.surface4.copy(alpha = 0.85f) else Color.Transparent)
                        .selectable(selected = on, role = Role.Tab) { onSelect(i) }
                        .semantics { contentDescription = item.label }
                        .animateContentSize(tween(PulsoMotion.BASE, easing = PulsoMotion.Standard))
                        .padding(horizontal = if (on) 14.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(item.icon, null, tint = if (on) c.text else c.text3, modifier = Modifier.size(21.dp))
                    if (on) Text(item.label, style = Pulso.type.meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = c.text, maxLines = 1)
                }
            }
        }
        if (onCreate != null) EmberFab(
            onClick = onCreate,
            contentDescription = createLabel,
            onLongClick = onCreateLongPress,
            onLongClickLabel = createLongPressLabel,
        )
    }
    }
}
