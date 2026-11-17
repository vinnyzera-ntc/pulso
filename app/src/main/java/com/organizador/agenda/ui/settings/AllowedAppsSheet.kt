package com.organizador.agenda.ui.settings

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.organizador.agenda.R
import com.organizador.agenda.domain.time.PtBr
import com.organizador.agenda.focus.FocusNotificationListener
import com.organizador.agenda.ui.common.IconTile
import com.organizador.agenda.ui.common.PulsoBottomSheet
import com.organizador.agenda.ui.common.PulsoTextField
import com.organizador.agenda.ui.pulso.Pulso
import com.organizador.agenda.ui.pulso.components.PulsoCard
import com.organizador.agenda.ui.pulso.components.SecondaryButton
import com.organizador.agenda.ui.pulso.components.SectionLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Um app da gaveta de apps. */
private data class LauncherApp(val packageName: String, val label: String)

/** Apps que aparecem na gaveta (o manifesto declara essa consulta em <queries>), sem o próprio Pulso. */
private fun loadLauncherApps(context: Context): List<LauncherApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    @Suppress("DEPRECATION") // a versão com ResolveInfoFlags é só do Android 13+.
    val found = pm.queryIntentActivities(intent, 0)
    return found
        .map { LauncherApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
}

/** Configurações de acesso às notificações (direto no Pulso a partir do Android 11). */
private fun Context.openListenerSettings() {
    val general = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
            Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
            ComponentName(this, FocusNotificationListener::class.java).flattenToString(),
        )
    } else {
        general
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(general)
    }
}

/**
 * Apps liberados no Não perturbe do foco: os da gaveta, com ícone e switch, busca e os já
 * liberados no topo (ordem fixada ao abrir, para a lista não pular a cada toque). Sem o acesso
 * às notificações, um painel explica e leva às configurações.
 */
@Composable
fun AllowedAppsSheet(
    allowed: Set<String>,
    listenerGranted: Boolean,
    onToggle: (packageName: String, allowed: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val c = Pulso.colors
    val apps by produceState<List<LauncherApp>?>(null) { value = withContext(Dispatchers.IO) { loadLauncherApps(context) } }
    val pinned = remember { allowed }
    var query by rememberSaveable { mutableStateOf("") }

    PulsoBottomSheet(onDismiss) {
        SectionLabel(stringResource(R.string.settings_dnd_allowed))
        Text(stringResource(R.string.dnd_apps_description), style = Pulso.type.bodyS, color = c.text2)
        if (!listenerGranted) {
            PulsoCard(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(Icons.Outlined.NotificationsActive)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.dnd_listener_title), style = Pulso.type.titleS, color = c.text)
                        Text(stringResource(R.string.dnd_listener_body), style = Pulso.type.bodyS, color = c.text2)
                    }
                }
                SecondaryButton(stringResource(R.string.dnd_listener_open), onClick = { context.openListenerSettings() }, modifier = Modifier.fillMaxWidth())
            }
        }
        PulsoTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.dnd_apps_search),
            modifier = Modifier.fillMaxWidth(),
        )
        val list = apps
        when {
            list == null -> Text(stringResource(R.string.dnd_apps_loading), style = Pulso.type.bodyS, color = c.text2)
            else -> {
                val q = query.trim()
                val shown = list
                    .filter { q.isEmpty() || it.label.contains(q, ignoreCase = true) }
                    .sortedWith(compareBy({ it.packageName !in pinned }, { it.label.lowercase(PtBr.locale) }))
                if (shown.isEmpty()) {
                    Text(stringResource(R.string.dnd_apps_empty), style = Pulso.type.bodyS, color = c.text2, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    SettingsGroup {
                        // Lista preguiçosa com altura limitada: os ícones só carregam para o que aparece.
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                            itemsIndexed(shown, key = { _, app -> app.packageName }) { i, app ->
                                if (i > 0) SettingsDivider()
                                SettingsToggleRow(
                                    title = app.label,
                                    checked = app.packageName in allowed,
                                    onCheckedChange = { onToggle(app.packageName, it) },
                                    leading = { AppIcon(app.packageName) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(packageName: String) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { 32.dp.roundToPx() }
    val icon by produceState<ImageBitmap?>(null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(px, px).asImageBitmap() }.getOrNull()
        }
    }
    Box(Modifier.size(32.dp)) {
        icon?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
