package com.organizador.agenda.ui.pulso

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight

/**
 * Liquid Glass do Pulso — só nas superfícies flutuantes (nav pill, aviso/snackbar, folhas),
 * decidido em 12/11/2026. O resto do app segue o Pulso opaco com hairlines.
 *
 * Vidro = o conteúdo de trás desfocado e refratado nas bordas (lente), com vibrância, um
 * véu da cor da superfície por cima (legibilidade) e o brilho especular no contorno no lugar
 * da hairline. Sem sombra: profundidade continua vindo das camadas (regra do Pulso).
 *
 * As telas registram o que fica por trás com `Modifier.layerBackdrop(...)` (feito no NavHost)
 * e expõem o registro em [LocalGlassBackdrop]. Sem registro, ou antes do Android 12, a
 * superfície cai no visual opaco de antes.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Desfoque por RenderEffect (Android 12+). A lente (refração) pede Android 13+; abaixo disso só desfoca. */
val glassSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Superfície de vidro sobre o conteúdo registrado em [LocalGlassBackdrop].
 * [veil] é a cor da superfície que vai por cima do vidro; [fallback] é a superfície opaca.
 */
@Composable
fun Modifier.glass(shape: Shape, veil: Color, fallback: Color, fallbackBorder: Color = Pulso.colors.line, blurDp: Int = 6): Modifier {
    val backdrop = LocalGlassBackdrop.current
    if (backdrop == null || !glassSupported) return this.background(fallback, shape).border(1.dp, fallbackBorder, shape)
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(blurDp.dp.toPx())
            lens(refractionHeight = 14.dp.toPx(), refractionAmount = 28.dp.toPx())
        },
        highlight = { Highlight.Default },
        shadow = null,
        onDrawSurface = { drawRect(veil) },
    )
}

/** Véu das superfícies de vidro: deixa ver o fundo, mas mantém o texto legível nos dois temas. */
val PulsoColors.glassVeil: Color get() = if (isDark) surface2.copy(alpha = 0.42f) else surface1.copy(alpha = 0.50f)

/**
 * Folhas abrem numa janela própria, fora do alcance do [LocalGlassBackdrop]: nelas o vidro é o
 * desfoque do sistema atrás da janela (Android 12+, se o aparelho permitir). Chamar dentro do
 * conteúdo da folha. Devolve true quando o desfoque está ativo (a folha pode ficar translúcida).
 */
@Composable
fun rememberWindowBlurBehind(radiusDp: Int = 28): Boolean {
    if (!glassSupported) return false
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: findDialogWindow(view) ?: return false
    val manager = view.context.getSystemService(WindowManager::class.java)
    val enabled = manager?.isCrossWindowBlurEnabled == true
    DisposableEffect(window, enabled) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = (radiusDp * view.resources.displayMetrics.density).toInt() }
        }
        onDispose { }
    }
    return enabled
}

private fun findDialogWindow(view: View): android.view.Window? {
    var p = view.parent
    while (p != null) {
        if (p is DialogWindowProvider) return p.window
        p = p.parent
    }
    return null
}
