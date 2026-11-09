package com.organizador.agenda.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.components.PulsoCard
import com.organizador.agenda.ui.pulso.components.SurfaceLevel

/** Grupo de linhas de ajuste: Panel (surface2 + hairline) com divisórias entre as linhas. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    PulsoCard(
        modifier.fillMaxWidth(),
        level = SurfaceLevel.Panel,
        contentPadding = PaddingValues(0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        content = content,
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = Pulso.colors.line, modifier = Modifier.padding(start = 14.dp))
}

/** Linha de 52dp: título (body) · valor (text2) · chevron. */
@Composable
fun SettingsRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    /** Valor embaixo do título (para valores longos, como o nome de uma agenda). */
    stacked: Boolean = false,
) {
    val c = Pulso.colors
    val valueContent: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            leading?.invoke()
            Text(value, style = Pulso.type.bodyS, color = c.text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (stacked) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = Pulso.type.body, color = c.text)
                valueContent()
            }
        } else {
            Text(title, style = Pulso.type.body, color = c.text, modifier = Modifier.weight(1f))
            // Valor compacto à direita, junto da seta; textos longos truncam.
            Box(Modifier.widthIn(max = 168.dp)) { valueContent() }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.text3, modifier = Modifier.size(20.dp))
    }
}

/** Linha com switch: a linha inteira alterna (role Switch), o Switch só desenha. */
@Composable
fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Pulso.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = Pulso.type.body, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = Pulso.type.meta, color = c.text2, maxLines = 1)
        }
        Switch(checked = checked, onCheckedChange = null, colors = pulsoSwitchColors())
    }
}

/** Switch Pulso: trilho ember quando ligado; surface4 + borda lineStrong quando desligado. */
@Composable
fun pulsoSwitchColors() = Pulso.colors.let { c ->
    SwitchDefaults.colors(
        checkedTrackColor = c.ember,
        checkedThumbColor = c.onEmber,
        checkedBorderColor = Color.Transparent,
        checkedIconColor = c.ember,
        uncheckedTrackColor = c.surface4,
        uncheckedThumbColor = c.text2,
        uncheckedBorderColor = c.lineStrong,
    )
}

/** Amostra da cor da agenda (cor do Google Agenda, que é dado do usuário). */
@Composable
fun CalendarSwatch(argb: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(Color(argb), RoundedCornerShape(3.dp)))
}
