package com.organizador.agenda.ui.common

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.components.SectionLabel

val CALENDAR_PERMISSIONS = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)

/** Textos do pedido de acesso à agenda (iguais em Ajustes, Agenda e na folha de compromisso). */
@Composable
fun calendarPermissionCopy() = PermissionCopy(
    title = stringResource(R.string.perm_calendar_title),
    message = stringResource(R.string.perm_calendar_message),
    requestLabel = stringResource(R.string.perm_calendar_request),
    deniedTitle = stringResource(R.string.perm_calendar_denied_title),
    deniedMessage = stringResource(R.string.perm_calendar_denied_message),
    blockedTitle = stringResource(R.string.perm_calendar_blocked_title),
    blockedMessage = stringResource(R.string.perm_calendar_blocked_message),
    openSettingsLabel = stringResource(R.string.perm_open_settings),
)

/** Amostra da cor de uma agenda (cor definida pelo usuário no Google Agenda: é dado, não tema). */
@Composable
fun CalendarDot(argb: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(Color(argb), RoundedCornerShape(3.dp)))
}

data class CalendarChoice(val id: Long, val name: String, val account: String, val color: Int)

/** Folha para escolher uma agenda; a escolhida leva o anel ember (seleção de valor). */
@Composable
fun CalendarChoiceSheet(
    title: String,
    calendars: List<CalendarChoice>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Pulso.colors
    PulsoBottomSheet(onDismiss) {
        SectionLabel(title)
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            calendars.forEach { cal ->
                val on = cal.id == selectedId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(if (on) c.emberSoft else c.surface2, PulsoShapes.control)
                        .border(if (on) 1.5.dp else 1.dp, if (on) c.ember else c.line, PulsoShapes.control)
                        .selectable(selected = on, role = Role.RadioButton) { onSelect(cal.id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CalendarDot(cal.color)
                    Column(Modifier.weight(1f)) {
                        Text(cal.name, style = Pulso.type.body, color = if (on) c.emberText else c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(cal.account, style = Pulso.type.meta, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (on) Icon(Icons.Outlined.Check, null, tint = c.emberText, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Botão secundário destrutivo: mesma forma do SecondaryButton do kit, rótulo em flareText. */
@Composable
fun DestructiveSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    Box(
        modifier
            .clip(PulsoShapes.button)
            .background(c.surface3)
            .border(1.dp, c.line, PulsoShapes.button)
            .clickable(indication = ripple(), interactionSource = null, role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = Pulso.type.button, color = c.flareText, maxLines = 1) }
}
