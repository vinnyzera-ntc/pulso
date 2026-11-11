package com.organizador.agenda.ui.pulso.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes

/** Tone follows the role map: Volt = time, Aurora = done, Flare = overdue, Neutral = everything else. */
enum class ChipTone { Neutral, Volt, Aurora, Flare }

/** Info chip (26dp): time, tag, reminder, location. Not a button unless onClick is set. */
@Composable
fun PulsoChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.Neutral,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Pulso.colors
    val (bg, fg, border) = when (tone) {
        ChipTone.Neutral -> Triple(c.surface3, c.text2, c.line)
        ChipTone.Volt -> Triple(c.voltSoft, c.voltText, Color.Transparent)
        ChipTone.Aurora -> Triple(c.auroraSoft, c.auroraText, Color.Transparent)
        ChipTone.Flare -> Triple(c.flareSoft, c.flareText, Color.Transparent)
    }
    Row(
        modifier
            .height(26.dp)
            .clip(PulsoShapes.chip)
            .background(bg)
            .border(1.dp, border, PulsoShapes.chip)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, style = Pulso.type.numeric, color = fg, maxLines = 1)
    }
}

/**
 * Quick-pick row (riff 02 "$50 $100 $500 $1,000"): equal-width options; selected = ember ring.
 * Use for durations, reminders, snooze, priorities.
 */
@Composable
fun <T> QuickChips(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
) {
    val c = Pulso.colors
    Row(modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(PulsoShapes.quick)
                    .background(if (on) c.emberSoft else c.surface3)
                    .border(if (on) 1.5.dp else 1.dp, if (on) c.ember else c.line, PulsoShapes.quick)
                    .selectable(selected = on, role = Role.RadioButton) { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), style = Pulso.type.button.copy(fontSize = Pulso.type.meta.fontSize), color = if (on) c.emberText else c.text)
            }
        }
    }
}

/** Segmented control (Dia / Semana / Mês). Selected = surface4, not ember: it's navigation, not action. */
@Composable
fun SegmentedPills(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pulso.colors
    Row(
        modifier
            .clip(PulsoShapes.control)
            .background(c.surface2)
            .border(1.dp, c.line, PulsoShapes.control)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp))
                    .background(if (on) c.surface4 else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab) { onSelect(i) }
                    .defaultMinSize(minWidth = 48.dp, minHeight = 42.dp)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = Pulso.type.meta, color = if (on) c.text else c.text3) }
        }
    }
}
