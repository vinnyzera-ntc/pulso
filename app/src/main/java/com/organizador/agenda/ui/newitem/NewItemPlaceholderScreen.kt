package com.organizador.agenda.ui.newitem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.ui.common.EmptyState
import com.organizador.agenda.ui.pulso.components.RoundIconButton

/** Temporário: a folha "Novo item" (ModalBottomSheet no padrão Quick Send) chega nas fases 2 e 3. */
@Composable
fun NewItemPlaceholderScreen(onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        RoundIconButton(Icons.Outlined.Close, stringResource(R.string.action_close), onClose, Modifier.padding(6.dp))
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                title = stringResource(R.string.action_new_item),
                message = stringResource(R.string.placeholder_new_item_body),
            )
        }
    }
}
