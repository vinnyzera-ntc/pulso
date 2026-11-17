package com.organizador.agenda.ui.focus

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.organizador.agenda.R
import com.organizador.agenda.ui.common.findActivity
import com.organizador.agenda.ui.pulso.Pulso

/** Playlist de música lofi para acompanhar o foco. */
const val LOFI_URL = "https://www.youtube.com/watch?v=rFZHOHl-L8A"

/**
 * Abre a playlist no YouTube (ou no navegador). Com o celular bloqueado (timer por cima da tela
 * de bloqueio), pede o desbloqueio antes: digital, rosto ou senha.
 */
fun Context.openLofi() {
    val activity = findActivity()
    val keyguard = getSystemService(KeyguardManager::class.java)
    if (activity != null && keyguard?.isKeyguardLocked == true) {
        keyguard.requestDismissKeyguard(
            activity,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = activity.startLofi()
            },
        )
    } else {
        startLofi()
    }
}

private fun Context.startLofi() {
    val intent = Intent(Intent.ACTION_VIEW, LOFI_URL.toUri())
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Sem YouTube nem navegador: nada a fazer.
    }
}

/** Logo do YouTube: retângulo arredondado vermelho com o triângulo branco (cores da marca, sem tinta). */
private val YouTubeGlyph: ImageVector by lazy {
    ImageVector.Builder(name = "YouTube", defaultWidth = 28.dp, defaultHeight = 20.dp, viewportWidth = 28f, viewportHeight = 20f)
        .path(fill = SolidColor(Color(0xFFFF0033))) {
            moveTo(6f, 0f)
            lineTo(22f, 0f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 28f, y1 = 6f)
            lineTo(28f, 14f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 22f, y1 = 20f)
            lineTo(6f, 20f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 0f, y1 = 14f)
            lineTo(0f, 6f)
            arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 6f, y1 = 0f)
            close()
        }
        .path(fill = SolidColor(Color.White)) {
            moveTo(11f, 5.5f)
            lineTo(19f, 10f)
            lineTo(11f, 14.5f)
            close()
        }
        .build()
}

/** Botão redondo (como o RoundIconButton) com o logo do YouTube: abre a playlist lofi. */
@Composable
fun LofiButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pulso.colors
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.surface1.copy(alpha = 0.7f)).border(1.dp, c.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(rememberVectorPainter(YouTubeGlyph), stringResource(R.string.focus_lofi), Modifier.size(width = 21.dp, height = 15.dp))
        }
    }
}
