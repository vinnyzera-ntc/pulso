package com.organizador.agenda.ui.pulso

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/* ---------- Shape & spacing ---------- */

object PulsoRadius {
    val chip = 9.dp       // info chips
    val quick = 11.dp     // quick-pick chips (15 / 30 / 45 / 1 h)
    val control = 12.dp   // fields, segmented controls, selectors
    val day = 14.dp       // week-strip day cells, event blocks
    val button = 16.dp    // full-width buttons
    val panel = 18.dp     // panel nested inside a card
    val card = 24.dp      // top-level cards
    val sheet = 30.dp     // bottom-sheet top corners
}

object PulsoShapes {
    val chip = RoundedCornerShape(PulsoRadius.chip)
    val quick = RoundedCornerShape(PulsoRadius.quick)
    val control = RoundedCornerShape(PulsoRadius.control)
    val day = RoundedCornerShape(PulsoRadius.day)
    val button = RoundedCornerShape(PulsoRadius.button)
    val panel = RoundedCornerShape(PulsoRadius.panel)
    val card = RoundedCornerShape(PulsoRadius.card)
    val sheet = RoundedCornerShape(topStart = PulsoRadius.sheet, topEnd = PulsoRadius.sheet)
    val pill = CircleShape
}

object PulsoSpace {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val gutter = 18.dp   // screen side padding
    val xl = 24.dp
    val xxl = 32.dp
    val hairline = 1.dp
    val ring = 1.5.dp    // selection ring / priority ring
}

/* ---------- Motion ---------- */

object PulsoMotion {
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)        // enter/move
    val Exit = CubicBezierEasing(0.3f, 0f, 1f, 1f)            // leave
    val Pulse = CubicBezierEasing(0.55f, 0f, 0.25f, 1f)       // signature: light travelling along a track
    const val QUICK = 120     // press, toggle
    const val BASE = 220      // most transitions
    const val SHEET = 320     // sheets, screen changes
    const val HALO = 700      // success ring, once
    const val LIVE = 1800     // live dot ring, loop
    const val PULSE = 2400    // now-line sweep, focus pulses, loop
    const val HIGHLIGHT = 3200 // capture gradient drift, loop, linear
}

/** True when the user disabled animations (Developer options / Accessibility "Remove animations"). */
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
private fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/* ---------- Theme ---------- */

@Composable
fun PulsoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val c = if (darkTheme) PulsoDark else PulsoLight
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = c.ember, onPrimary = c.onEmber, primaryContainer = c.emberSoft, onPrimaryContainer = c.emberText,
            secondary = c.volt, onSecondary = Color(0xFF03111F), secondaryContainer = c.voltSoft, onSecondaryContainer = c.voltText,
            tertiary = c.aurora, onTertiary = c.onAurora, tertiaryContainer = c.auroraSoft, onTertiaryContainer = c.auroraText,
            error = c.flare, onError = Color(0xFF1F0306), errorContainer = c.flareSoft, onErrorContainer = c.flareText,
            background = c.bg, onBackground = c.text, surface = c.bg, onSurface = c.text,
            surfaceVariant = c.surface3, onSurfaceVariant = c.text2, surfaceTint = Color.Transparent,
            outline = c.lineStrong, outlineVariant = c.line, scrim = c.scrim,
            surfaceContainerLowest = c.bg, surfaceContainerLow = c.surface1, surfaceContainer = c.surface2,
            surfaceContainerHigh = c.surface3, surfaceContainerHighest = c.surface4,
            inverseSurface = PulsoLight.surface1, inverseOnSurface = PulsoLight.text, inversePrimary = PulsoLight.ember,
        )
    } else {
        lightColorScheme(
            primary = c.ember, onPrimary = c.onEmber, primaryContainer = c.emberSoft, onPrimaryContainer = c.emberText,
            secondary = c.volt, onSecondary = Color.White, secondaryContainer = c.voltSoft, onSecondaryContainer = c.voltText,
            tertiary = c.aurora, onTertiary = c.onAurora, tertiaryContainer = c.auroraSoft, onTertiaryContainer = c.auroraText,
            error = c.flare, onError = Color.White, errorContainer = c.flareSoft, onErrorContainer = c.flareText,
            background = c.bg, onBackground = c.text, surface = c.bg, onSurface = c.text,
            surfaceVariant = c.surface3, onSurfaceVariant = c.text2, surfaceTint = Color.Transparent,
            outline = c.lineStrong, outlineVariant = c.line, scrim = c.scrim,
            surfaceContainerLowest = c.surface1, surfaceContainerLow = c.surface2, surfaceContainer = c.surface1,
            surfaceContainerHigh = c.surface3, surfaceContainerHighest = c.surface4,
            inverseSurface = PulsoDark.surface1, inverseOnSurface = PulsoDark.text, inversePrimary = PulsoDark.ember,
        )
    }
    val shapes = Shapes(
        extraSmall = PulsoShapes.chip, small = PulsoShapes.control, medium = PulsoShapes.panel,
        large = PulsoShapes.card, extraLarge = PulsoShapes.sheet,
    )
    CompositionLocalProvider(
        LocalPulsoColors provides c,
        LocalPulsoType provides PulsoTypeDefault,
        LocalReducedMotion provides rememberReducedMotion(),
    ) {
        MaterialTheme(colorScheme = scheme, typography = PulsoTypeDefault.toMaterial(), shapes = shapes, content = content)
    }
}

/** Accessors: `Pulso.colors.ember`, `Pulso.type.headline`. */
object Pulso {
    val colors: PulsoColors
        @Composable @ReadOnlyComposable get() = LocalPulsoColors.current
    val type: PulsoType
        @Composable @ReadOnlyComposable get() = LocalPulsoType.current
    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducedMotion.current
}
