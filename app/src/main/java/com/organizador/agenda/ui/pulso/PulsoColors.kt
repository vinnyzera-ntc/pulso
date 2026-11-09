package com.organizador.agenda.ui.pulso

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Pulso color tokens. One accent per role:
 *  - ember  = action (primary CTA, create, selection, typing caret)
 *  - volt   = time (now line, live/in-progress, today, focus, reminders, sync)
 *  - aurora = result (done, success, progress) + the mint atmosphere glow
 *  - flare  = alert (overdue, P1, destructive)
 * Text-on-surface variants (`*Text`) exist because the raw accents fail 4.5:1 on light surfaces.
 */
@Immutable
data class PulsoColors(
    // Surfaces (tonal layering, no elevation shadows)
    val bg: Color,
    val surface1: Color,   // cards, sheets
    val surface2: Color,   // panels nested in cards, nav pill
    val surface3: Color,   // chips, fields, secondary buttons
    val surface4: Color,   // avatars, selected nav item, empty progress
    val line: Color,       // 1dp hairline on every surface
    val lineStrong: Color, // unchecked checkbox, dashed "add", sheet top edge
    // Text
    val text: Color,
    val text2: Color,      // secondary text, labels
    val text3: Color,      // meta only (hour labels, disabled, done tasks) — ~3.4:1, never for essential copy
    // Ember — action
    val ember: Color,
    val emberHi: Color,    // top stop of the CTA gradient
    val onEmber: Color,    // text/icons on ember (dark ink: white fails contrast)
    val emberText: Color,  // ember used as text on surfaces
    val emberSoft: Color,  // selected fill
    // Volt — time
    val volt: Color,
    val voltGlow: Color,   // glow / pulse light (riff 01 electric blue)
    val voltText: Color,
    val voltSoft: Color,
    // Aurora — result
    val aurora: Color,
    val auroraHi: Color,
    val onAurora: Color,
    val auroraText: Color,
    val auroraSoft: Color,
    val mint: Color,             // atmosphere glow color
    val auroraIntensity: Float,  // alpha of the top-of-screen glow
    // Flare — alert
    val flare: Color,
    val flareText: Color,
    val flareSoft: Color,
    // Misc
    val scrim: Color,
    val deep: Color,       // focus-mode background
    val isDark: Boolean,
) {
    /** Smart-capture highlight (riff 03): mint → aurora → volt. Reserved for recognized text in capture fields. */
    val captureGradient: List<Color> get() = listOf(mint, aurora, voltGlow)
}

val PulsoDark = PulsoColors(
    bg = Color(0xFF0E0E0F),
    surface1 = Color(0xFF141415),
    surface2 = Color(0xFF19191B),
    surface3 = Color(0xFF202022),
    surface4 = Color(0xFF2A2A2D),
    line = Color(0xFF242427),
    lineStrong = Color(0xFF3A3A3E),
    text = Color(0xFFF4F4F5),
    text2 = Color(0xFFA1A1A7),
    text3 = Color(0xFF6E6E75),
    ember = Color(0xFFFF7631),
    emberHi = Color(0xFFFF8C4F),
    onEmber = Color(0xFF170A04),
    emberText = Color(0xFFFF7631),
    emberSoft = Color(0x24FF7631),
    volt = Color(0xFF2F95F5),
    voltGlow = Color(0xFF0C84ED),
    voltText = Color(0xFF2F95F5),
    voltSoft = Color(0x242F95F5),
    aurora = Color(0xFF31B66F),
    auroraHi = Color(0xFF46D488),
    onAurora = Color(0xFF04150B),
    auroraText = Color(0xFF31B66F),
    auroraSoft = Color(0x2931B66F),
    mint = Color(0xFF98C397),
    auroraIntensity = 0.20f,
    flare = Color(0xFFFF4F5E),
    flareText = Color(0xFFFF4F5E),
    flareSoft = Color(0x24FF4F5E),
    scrim = Color(0x9E000000),
    deep = Color(0xFF050505),
    isDark = true,
)

val PulsoLight = PulsoColors(
    bg = Color(0xFFF2F4F3),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF7F9F8),
    surface3 = Color(0xFFECEFEE),
    surface4 = Color(0xFFE1E6E4),
    line = Color(0xFFE0E5E3),
    lineStrong = Color(0xFFC9D0CD),
    text = Color(0xFF0E1413),
    text2 = Color(0xFF4D5856),
    text3 = Color(0xFF7C8683),
    ember = Color(0xFFFF6B22),
    emberHi = Color(0xFFFF8644),
    onEmber = Color(0xFF160904),
    emberText = Color(0xFFB8420A),
    emberSoft = Color(0x1FFF6B22),
    volt = Color(0xFF0B6FD3),
    voltGlow = Color(0xFF0C84ED),
    voltText = Color(0xFF0A5FB8),
    voltSoft = Color(0x1A0B6FD3),
    aurora = Color(0xFF1E9C5A),
    auroraHi = Color(0xFF2DB86E),
    onAurora = Color(0xFF04150B),
    auroraText = Color(0xFF13753F),
    auroraSoft = Color(0x1F1E9C5A),
    mint = Color(0xFF98C397),
    auroraIntensity = 0.42f,
    flare = Color(0xFFE2334A),
    flareText = Color(0xFFC21F35),
    flareSoft = Color(0x1AE2334A),
    scrim = Color(0x610E1413),
    deep = Color(0xFFE9EDEB),
    isDark = false,
)

val LocalPulsoColors = staticCompositionLocalOf { PulsoDark }
