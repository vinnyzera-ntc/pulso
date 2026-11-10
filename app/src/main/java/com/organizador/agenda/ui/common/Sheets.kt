package com.organizador.agenda.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.QuickChips
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import java.time.LocalTime

/**
 * ModalBottomSheet no traje Pulso: surface1, raio 30, borda superior lineStrong,
 * alça 38×4, scrim do tema, padding 16.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsoBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pulso.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // O fundo e a borda lineStrong são desenhados no conteúdo: uma borda no modifier da
        // folha fica presa à posição inicial e aparece como uma linha solta na tela.
        containerColor = Color.Transparent,
        contentColor = c.text,
        scrimColor = c.scrim,
        shape = PulsoShapes.sheet,
        tonalElevation = 0.dp,
        dragHandle = null,
        modifier = Modifier.statusBarsPadding(),
        // A própria folha aplica o recuo da barra de gestos, com o fundo indo até a borda da tela.
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.surface1, PulsoShapes.sheet)
                .border(1.dp, c.lineStrong, PulsoShapes.sheet),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(c.lineStrong, RoundedCornerShape(2.dp)),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/**
 * Escolha de um valor entre poucas opções (QuickChips, anel ember no selecionado).
 * Tocar numa opção já aplica e fecha: a escolha é a ação.
 */
@Composable
fun <T> ChoiceSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    description: String? = null,
) {
    PulsoBottomSheet(onDismiss) {
        SectionLabel(title)
        if (description != null) {
            Text(description, style = Pulso.type.bodyS, color = Pulso.colors.text2)
        }
        QuickChips(
            options = options,
            selected = selected,
            onSelect = {
                onSelect(it)
                onDismiss()
            },
            label = label,
        )
    }
}

/** TimePicker 24h restilizado (dial surface3, seletor ember), com o resultado no botão. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulsoTimePickerDialog(
    title: String,
    initial: LocalTime,
    confirmLabel: (LocalTime) -> String,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Pulso.colors
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .background(c.surface1, PulsoShapes.card)
                .border(1.dp, c.line, PulsoShapes.card)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = Pulso.type.title, color = c.text, modifier = Modifier.fillMaxWidth())
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.surface3,
                    clockDialSelectedContentColor = c.onEmber,
                    clockDialUnselectedContentColor = c.text,
                    selectorColor = c.ember,
                    containerColor = c.surface1,
                    timeSelectorSelectedContainerColor = c.emberSoft,
                    timeSelectorUnselectedContainerColor = c.surface3,
                    timeSelectorSelectedContentColor = c.emberText,
                    timeSelectorUnselectedContentColor = c.text,
                ),
            )
            val picked = LocalTime.of(state.hour, state.minute)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SecondaryButton("Cancelar", onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(confirmLabel(picked), onClick = { onConfirm(picked) }, modifier = Modifier.weight(1.4f))
            }
        }
    }
}

/** Rótulo padrão do botão de confirmação de horário: "Usar 09:00". */
fun useTimeLabel(t: LocalTime): String = "Usar ${PtBr.time(t)}"
