package com.organizador.agenda.ui.pulso.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.organizador.agenda.ui.pulso.Pulso

data class Person(val id: String, val name: String) {
    val initials: String get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

/** Initials avatar. `selected` = ember ring + ember-soft fill (the riff 02 selection). */
@Composable
fun Avatar(person: Person, modifier: Modifier = Modifier, size: Dp = 30.dp, selected: Boolean = false) {
    val c = Pulso.colors
    Box(
        modifier
            .size(size)
            .then(if (selected) Modifier.drawBehind {
                drawCircle(c.ember.copy(alpha = 0.10f), radius = this.size.minDimension / 2f + 4.dp.toPx())
            } else Modifier)
            .clip(CircleShape)
            .background(if (selected) c.emberSoft else c.surface4)
            .then(if (selected) Modifier.border(2.dp, c.ember, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            person.initials,
            style = Pulso.type.meta.copy(fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.34f).sp),
            color = if (selected) c.emberText else c.text,
        )
    }
}

/** Overlapping avatar stack: "TH VR +4". */
@Composable
fun AvatarStack(people: List<Person>, modifier: Modifier = Modifier, max: Int = 2, size: Dp = 28.dp) {
    val c = Pulso.colors
    Row(modifier) {
        people.take(max).forEachIndexed { i, p ->
            Avatar(p, Modifier.offset(x = (-8).dp * i).border(2.dp, c.surface1, CircleShape), size)
        }
        if (people.size > max) {
            Box(
                Modifier.offset(x = (-8).dp * max).size(size).clip(CircleShape).background(c.surface4).border(2.dp, c.surface1, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("+${people.size - max}", style = Pulso.type.meta.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = c.text) }
        }
    }
}

/**
 * Person picker (riff 02 Quick Send contacts): dashed "Adicionar" first, then 50dp avatars with names.
 * Multi-select for attendees/assignees.
 */
@Composable
fun PersonPicker(
    people: List<Person>,
    selectedIds: Set<String>,
    onToggle: (Person) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    addLabel: String = "Adicionar",
) {
    val c = Pulso.colors
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(
                Modifier.width(58.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onAdd),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Box(
                    Modifier.size(50.dp).drawBehind {
                        drawCircle(
                            c.lineStrong, radius = size.minDimension / 2f - 1.dp.toPx(),
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                        )
                    },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, null, tint = c.text2, modifier = Modifier.size(20.dp)) }
                Text(addLabel, style = Pulso.type.meta, color = c.text2, maxLines = 1)
            }
        }
        items(people, key = { it.id }) { p ->
            val on = p.id in selectedIds
            Column(
                Modifier.width(58.dp).toggleable(value = on, role = Role.Checkbox) { onToggle(p) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Avatar(p, size = 50.dp, selected = on)
                Text(
                    p.name.substringBefore(" "),
                    style = Pulso.type.meta.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium),
                    color = if (on) c.text else c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
