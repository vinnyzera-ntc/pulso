package com.organizador.agenda.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.PulsoCard
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SurfaceLevel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Campo de texto Pulso: surface2 + hairline, raio 12, 52dp, rótulo acima em `meta`
 * (sem rótulo flutuante); foco = borda lineStrong + cursor ember.
 */
@Composable
fun PulsoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    textStyle: TextStyle = Pulso.type.body,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
) {
    val c = Pulso.colors
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, style = Pulso.type.meta, color = c.text2)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = textStyle.copy(color = c.text),
            cursorBrush = SolidColor(c.ember),
            interactionSource = source,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(c.surface2, PulsoShapes.control)
                        .border(1.dp, if (focused) c.lineStrong else c.line, PulsoShapes.control)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = textStyle, color = c.text3)
                    }
                    inner()
                }
            },
        )
    }
}

/** Painel-campo (data | hora): ícone + valor, abre um seletor ao tocar. */
@Composable
fun FieldPanel(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPlaceholder: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Pulso.colors
    PulsoCard(
        modifier.heightIn(min = 52.dp),
        level = SurfaceLevel.Panel,
        onClick = if (enabled) onClick else null,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = if (trailing != null) 2.dp else 14.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 50.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(icon, null, tint = if (enabled) c.text2 else c.text3, modifier = Modifier.size(18.dp))
            Text(
                text,
                style = Pulso.type.body,
                color = if (!enabled || isPlaceholder) c.text3 else c.text,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
        }
    }
}

/** Chip de escolha (40dp, raio 11): selecionado = anel ember + emberSoft (seleção de valor). */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
) {
    val c = Pulso.colors
    Row(
        modifier
            .heightIn(min = 40.dp)
            .clip(PulsoShapes.quick)
            .background(if (selected) c.emberSoft else c.surface3)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.ember else c.line, PulsoShapes.quick)
            .toggleable(value = selected, role = Role.Checkbox) { onClick() }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text, style = Pulso.type.button.copy(fontSize = Pulso.type.meta.fontSize), color = if (selected) c.emberText else c.text, maxLines = 1)
        if (trailingText != null) {
            Text(trailingText, style = Pulso.type.numeric, color = c.flareText)
        }
    }
}

/** DatePicker do Material 3 no traje Pulso; o botão diz o resultado ("Usar Ter, 10 nov"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsoDatePickerDialog(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Pulso.colors
    // Em tela baixa (paisagem) a grade do mês não cabe: a data é digitada.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        initialDisplayMode = if (isCompactHeight()) DisplayMode.Input else DisplayMode.Picker,
    )
    val picked = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
    val colors = DatePickerDefaults.colors(
        containerColor = c.surface1,
        titleContentColor = c.text2,
        headlineContentColor = c.text,
        weekdayContentColor = c.text3,
        subheadContentColor = c.text2,
        navigationContentColor = c.text,
        yearContentColor = c.text,
        currentYearContentColor = c.voltText,
        selectedYearContainerColor = c.ember,
        selectedYearContentColor = c.onEmber,
        dayContentColor = c.text,
        selectedDayContainerColor = c.ember,
        selectedDayContentColor = c.onEmber,
        todayContentColor = c.voltText,
        todayDateBorderColor = c.volt,
        dividerColor = c.line,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = colors,
        shape = PulsoShapes.card,
        tonalElevation = 0.dp,
        confirmButton = {
            PrimaryButton(
                text = picked?.let { "Usar ${PtBr.shortDate(it)}" } ?: "Usar data",
                onClick = { picked?.let(onConfirm) },
                enabled = picked != null,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp),
            )
        },
        dismissButton = {
            SecondaryButton("Cancelar", onClick = onDismiss, modifier = Modifier.padding(bottom = 8.dp))
        },
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}
