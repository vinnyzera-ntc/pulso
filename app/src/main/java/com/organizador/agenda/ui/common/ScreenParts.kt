package com.organizador.agenda.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoSpace

/**
 * Tela com pouca altura (celular em paisagem). Nela, o que é alto demais muda de forma:
 * seletores de data e hora viram campos de digitação e botões fixos passam a rolar.
 */
@Composable
@ReadOnlyComposable
fun isCompactHeight(): Boolean = LocalConfiguration.current.screenHeightDp < 480

/**
 * Cabeçalho de tela Pulso: eyebrow opcional (caixa alta, text2) + título em `headline`,
 * ações à direita como RoundIconButton. Sem barra colorida.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Pulso.colors
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = PulsoSpace.gutter, end = 8.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow.uppercase(PtBr.locale), style = Pulso.type.eyebrow, color = c.text2)
            }
            Text(
                title,
                style = Pulso.type.headline,
                color = c.text,
                modifier = Modifier
                    .padding(top = if (eyebrow != null) 4.dp else 0.dp)
                    .semantics { heading() },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/**
 * Estado vazio (patterns §9): título em `title` + uma frase em text2, centralizados,
 * com a ação principal opcional abaixo. A aurora fica a cargo da tela (só telas hero).
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val c = Pulso.colors
    Column(
        modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = Pulso.type.title, color = c.text, textAlign = TextAlign.Center)
        Text(message, style = Pulso.type.bodyS, color = c.text2, textAlign = TextAlign.Center)
        if (action != null) {
            Column(Modifier.padding(top = 16.dp).fillMaxWidth()) { action() }
        }
    }
}
