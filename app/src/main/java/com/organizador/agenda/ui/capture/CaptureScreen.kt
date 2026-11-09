package com.organizador.agenda.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoSpace
import com.organizador.agenda.ui.pulso.components.RoundIconButton
import com.organizador.agenda.ui.pulso.components.aurora

/** Placeholder da Captura rápida (fase 4). Tela hero: aurora mais forte (+0.25). */
@Composable
fun CaptureScreen(onClose: () -> Unit) {
    val c = Pulso.colors
    Box(Modifier.fillMaxSize().aurora(c, intensity = c.auroraIntensity + 0.25f)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                RoundIconButton(Icons.Outlined.Close, stringResource(R.string.action_close), onClose)
                Text(
                    stringResource(R.string.action_quick_capture).uppercase(PtBr.locale),
                    style = Pulso.type.eyebrow,
                    color = c.text2,
                )
                Spacer(Modifier.size(48.dp))
            }
            Text(
                stringResource(R.string.capture_prompt),
                style = Pulso.type.headline,
                color = c.text,
                modifier = Modifier.padding(horizontal = PulsoSpace.gutter, vertical = 20.dp),
            )
            Text(
                stringResource(R.string.placeholder_capture_body),
                style = Pulso.type.bodyS,
                color = c.text2,
                modifier = Modifier.padding(horizontal = PulsoSpace.gutter),
            )
        }
    }
}
