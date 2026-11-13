package com.organizador.agenda.ui.common

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.di.ApplicationScope
import com.organizador.agenda.ui.pulso.Pulso
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Mensagem curta para o snackbar, com ação opcional (tipicamente "Desfazer"). */
data class UiMessage(
    @param:StringRes val text: Int,
    @param:StringRes val actionLabel: Int? = null,
    /** Argumentos de formatação do texto. */
    val args: List<Any> = emptyList(),
    /**
     * Roda quando o aviso sai da tela sem a ação ter sido tocada (tempo esgotado, dispensado ou
     * substituído por outro aviso). Serve para efetivar algo que ficou "em espera" pelo Desfazer.
     */
    val onDismissed: (suspend () -> Unit)? = null,
    /** A ação do botão (por último, para poder ser passada como lambda final). */
    val action: (suspend () -> Unit)? = null,
)

/**
 * Canal único de mensagens do app. Os ViewModels publicam; o NavHost mostra o snackbar.
 * A ação roda no escopo da aplicação, então "Desfazer" funciona mesmo depois de a tela
 * que publicou a mensagem ter sido fechada (ex.: excluir uma tarefa na tela de detalhe).
 */
@Singleton
class Messenger @Inject constructor(@param:ApplicationScope private val scope: CoroutineScope) {
    private val channel = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = channel.receiveAsFlow()

    fun show(message: UiMessage) {
        channel.trySend(message)
    }

    fun runAction(message: UiMessage) {
        val action = message.action ?: return
        scope.launch { guard { action() } }
    }

    fun runDismissed(message: UiMessage) {
        val onDismissed = message.onDismissed ?: return
        scope.launch { guard { onDismissed() } }
    }

    /**
     * Roda [block]; se falhar (ex.: a permissão da agenda foi revogada, a agenda sumiu), avisa
     * com [failure] em vez de derrubar o app e devolve null. Cancelamento passa direto.
     */
    suspend fun <T> guard(@StringRes failure: Int = R.string.msg_action_failed, block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Pulso", "Falha numa ação", e)
            show(UiMessage(failure))
            null
        }
}

/** Snackbar Pulso: surface3 + hairline, raio 14, texto + ação em emberText. */
@Composable
fun PulsoSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data -> PulsoSnackbar(data) }
}

@Composable
private fun PulsoSnackbar(data: SnackbarData) {
    val c = Pulso.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(c.surface3, shape)
            .border(1.dp, c.line, shape)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(data.visuals.message, style = Pulso.type.bodyS, color = c.text, modifier = Modifier.weight(1f))
        data.visuals.actionLabel?.let { label ->
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button) { data.performAction() }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Pulso.type.bodyS.copy(fontWeight = FontWeight.SemiBold), color = c.emberText)
            }
        }
    }
}
