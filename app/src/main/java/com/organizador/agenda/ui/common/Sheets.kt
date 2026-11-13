package com.organizador.agenda.ui.common

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.pulso.LocalGlassBackdrop
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.QuickChips
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import com.organizador.agenda.ui.pulso.glass
import com.organizador.agenda.ui.pulso.glassSupported
import com.organizador.agenda.ui.pulso.rememberWindowBlurBehind
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
    // Liquid Glass nas folhas: o fundo do app desfocado pelo sistema, a folha translúcida por cima.
    val context = LocalContext.current
    val blurAvailable = glassSupported && context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled == true
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // O fundo e a borda lineStrong são desenhados no conteúdo: uma borda no modifier da
        // folha fica presa à posição inicial e aparece como uma linha solta na tela.
        containerColor = Color.Transparent,
        contentColor = c.text,
        // Com desfoque, o scrim fica mais leve: o desfoque já separa a folha do fundo.
        scrimColor = if (blurAvailable) c.scrim.copy(alpha = c.scrim.alpha * 0.55f) else c.scrim,
        shape = PulsoShapes.sheet,
        tonalElevation = 0.dp,
        dragHandle = null,
        modifier = Modifier.statusBarsPadding(),
        // A própria folha aplica o recuo da barra de gestos, com o fundo indo até a borda da tela.
        contentWindowInsets = { WindowInsets(0) },
    ) {
        val blurred = rememberWindowBlurBehind()
        Column(
            Modifier
                .fillMaxWidth()
                .then(
                    when {
                        // Vidro sobre o conteúdo do app (o mesmo registro do nav pill); véu mais denso: a folha é cheia de texto.
                        LocalGlassBackdrop.current != null && glassSupported ->
                            Modifier.glass(PulsoShapes.sheet, veil = c.surface1.copy(alpha = if (c.isDark) 0.74f else 0.80f), fallback = c.surface1, blurDp = 14)
                        blurred -> Modifier
                            .background(c.surface1.copy(alpha = if (c.isDark) 0.80f else 0.84f), PulsoShapes.sheet)
                            .border(1.dp, c.lineStrong, PulsoShapes.sheet)
                        else -> Modifier.background(c.surface1, PulsoShapes.sheet).border(1.dp, c.lineStrong, PulsoShapes.sheet)
                    },
                ),
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
    /** Lista vertical (opções de texto longo, como as de repetição) em vez da fileira de chips. */
    vertical: Boolean = false,
) {
    PulsoBottomSheet(onDismiss) {
        SectionLabel(title)
        if (description != null) {
            Text(description, style = Pulso.type.bodyS, color = Pulso.colors.text2)
        }
        val pick: (T) -> Unit = {
            onSelect(it)
            onDismiss()
        }
        if (vertical) {
            Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option -> ChoiceRow(label(option), option == selected) { pick(option) } }
            }
        } else {
            QuickChips(options = options, selected = selected, onSelect = pick, label = label)
        }
    }
}

/** Opção de lista: seleção de valor, então o anel ember marca a escolhida (como no Foco). */
@Composable
private fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit) {
    val c = Pulso.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(if (selected) c.emberSoft else c.surface2, PulsoShapes.control)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.ember else c.line, PulsoShapes.control)
            .clip(PulsoShapes.control)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Pulso.type.body, color = if (selected) c.emberText else c.text, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Outlined.Check, null, tint = c.emberText, modifier = Modifier.size(20.dp))
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = Pulso.type.title, color = c.text, modifier = Modifier.fillMaxWidth())
            val timeColors = TimePickerDefaults.colors(
                clockDialColor = c.surface3,
                clockDialSelectedContentColor = c.onEmber,
                clockDialUnselectedContentColor = c.text,
                selectorColor = c.ember,
                containerColor = c.surface1,
                timeSelectorSelectedContainerColor = c.emberSoft,
                timeSelectorUnselectedContainerColor = c.surface3,
                timeSelectorSelectedContentColor = c.emberText,
                timeSelectorUnselectedContentColor = c.text,
            )
            // Em tela baixa (paisagem) o mostrador não cabe: a hora é digitada.
            if (isCompactHeight()) TimeInput(state = state, colors = timeColors)
            else TimePicker(state = state, colors = timeColors)
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
