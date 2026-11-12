package com.organizador.agenda.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.PulsoShapes
import com.organizador.agenda.ui.pulso.components.PrimaryButton
import com.organizador.agenda.ui.pulso.components.PulsoCard
import com.organizador.agenda.ui.pulso.components.SecondaryButton

/**
 * Estado de uma permissão de runtime:
 * - [NotRequested]: nunca pedida — mostrar a explicação e o botão que dispara o pedido.
 * - [Denied]: negada, o Android ainda deixa pedir de novo.
 * - [Blocked]: negada de vez; só pelas configurações do app.
 */
enum class PermissionStatus { Granted, NotRequested, Denied, Blocked }

@Stable
class PermissionRequest internal constructor(
    val status: PermissionStatus,
    val launch: () -> Unit,
    val openSettings: () -> Unit,
)

/**
 * Controla o pedido de [permissions] (todas juntas). [alreadyRequested] vem persistido, porque
 * `shouldShowRequestPermissionRationale` é false tanto antes do primeiro pedido quanto depois do
 * bloqueio. O status é recalculado ao voltar das configurações (ON_RESUME).
 */
@Composable
fun rememberPermissionRequest(
    permissions: List<String>,
    alreadyRequested: Boolean,
    onRequested: () -> Unit,
    onResult: (granted: Boolean) -> Unit,
): PermissionRequest {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        tick++
        currentOnResult(result.values.all { it })
    }
    val status = remember(tick, alreadyRequested, permissions) { context.permissionStatus(permissions, alreadyRequested) }
    return PermissionRequest(
        status = status,
        launch = {
            onRequested()
            launcher.launch(permissions.toTypedArray())
        },
        openSettings = { context.openAppSettings() },
    )
}

private fun Context.permissionStatus(permissions: List<String>, alreadyRequested: Boolean): PermissionStatus {
    if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
        return PermissionStatus.Granted
    }
    if (!alreadyRequested) return PermissionStatus.NotRequested
    val activity = findActivity() ?: return PermissionStatus.Denied
    return if (permissions.any { activity.shouldShowRequestPermissionRationale(it) }) PermissionStatus.Denied
    else PermissionStatus.Blocked
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Textos de um pedido de permissão em cada estado. */
data class PermissionCopy(
    val title: String,
    val message: String,
    val requestLabel: String,
    val deniedTitle: String,
    val deniedMessage: String,
    val blockedTitle: String,
    val blockedMessage: String,
    val openSettingsLabel: String,
)

/**
 * Painel de permissão: explica por que o acesso é necessário antes do diálogo do sistema e,
 * se negado, diz o que deixa de funcionar e como liberar. Não usa flare: negar não é erro.
 * O botão de pedido é a ação principal (ember); o de configurações é secundário.
 */
@Composable
fun PermissionPanel(
    request: PermissionRequest,
    icon: ImageVector,
    copy: PermissionCopy,
    modifier: Modifier = Modifier,
    /** false quando a tela já tem a sua ação principal (ember): o pedido vira botão secundário. */
    primaryRequest: Boolean = true,
) {
    val c = Pulso.colors
    val (title, message) = when (request.status) {
        PermissionStatus.Denied -> copy.deniedTitle to copy.deniedMessage
        PermissionStatus.Blocked -> copy.blockedTitle to copy.blockedMessage
        else -> copy.title to copy.message
    }
    PulsoCard(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = Pulso.type.titleS, color = c.text)
                Text(message, style = Pulso.type.bodyS, color = c.text2)
            }
        }
        when (request.status) {
            PermissionStatus.Blocked ->
                SecondaryButton(copy.openSettingsLabel, onClick = request.openSettings, modifier = Modifier.fillMaxWidth())
            PermissionStatus.Granted -> Unit
            else ->
                if (primaryRequest) PrimaryButton(copy.requestLabel, onClick = request.launch, modifier = Modifier.fillMaxWidth())
                else SecondaryButton(copy.requestLabel, onClick = request.launch, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Ícone num quadrado neutro (surface3 + hairline), como os tiles do kit. */
@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    Box(
        modifier
            .size(40.dp)
            .background(c.surface3, PulsoShapes.control)
            .border(1.dp, c.line, PulsoShapes.control),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = c.text2, modifier = Modifier.size(20.dp)) }
}
