package com.organizador.agenda.ui.pulso

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.organizador.agenda.R

/*
 * Fonts: copy assets/fonts/urbanist_variable.ttf and manrope_variable.ttf (OFL) into res/font/.
 * Urbanist = display (titles, big static numbers). Manrope = text, labels, and any number that
 * ticks or aligns in columns (Urbanist has no tabular figures; Manrope does, via "tnum").
 */
@OptIn(ExperimentalTextApi::class)
private fun urbanist(w: Int) = Font(
    R.font.urbanist_variable, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

@OptIn(ExperimentalTextApi::class)
private fun manrope(w: Int) = Font(
    R.font.manrope_variable, FontWeight(w),
    variationSettings = FontVariation.Settings(FontVariation.weight(w)),
)

val Urbanist = FontFamily(urbanist(400), urbanist(500), urbanist(600), urbanist(700))
val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700))

@Immutable
data class PulsoType(
    val displayL: TextStyle,  // 52 — big static value: duration "45", amount
    val displayM: TextStyle,  // 32 — confirmation value "Ter, 10 nov · 14:00"
    val headline: TextStyle,  // 27 — screen title / greeting
    val title: TextStyle,     // 21 — sheet title, empty-state title
    val titleS: TextStyle,    // 16 — card title, event title
    val body: TextStyle,      // 15 — task title, list primary
    val bodyS: TextStyle,     // 14 — secondary paragraphs
    val meta: TextStyle,      // 12.5 — chips, times, captions
    val eyebrow: TextStyle,   // 11.5 caps — section labels (call .uppercase() on the string)
    val button: TextStyle,    // 15 — button labels
    val timer: TextStyle,     // 34 tabular — ticking numbers (focus timer, countdown)
    val numeric: TextStyle,   // 12.5 tabular — hour column, times in lists
)

private val tnum = "tnum"

val PulsoTypeDefault = PulsoType(
    displayL = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-0.02).em),
    displayM = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.015).em),
    headline = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 31.sp, letterSpacing = (-0.015).em),
    title = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    titleS = TextStyle(fontFamily = Urbanist, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp),
    body = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    bodyS = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    meta = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 16.sp),
    eyebrow = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 11.5.sp, lineHeight = 14.sp, letterSpacing = 0.09.em),
    button = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    timer = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.01).em, fontFeatureSettings = tnum),
    numeric = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, lineHeight = 16.sp, fontFeatureSettings = tnum),
)

val LocalPulsoType = staticCompositionLocalOf { PulsoTypeDefault }

/** Material 3 mapping so stock M3 components inherit Pulso type. */
fun PulsoType.toMaterial() = Typography(
    displayLarge = displayL,
    displayMedium = displayM,
    displaySmall = displayM.copy(fontSize = 28.sp, lineHeight = 32.sp),
    headlineLarge = headline.copy(fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = headline,
    headlineSmall = title,
    titleLarge = title,
    titleMedium = titleS,
    titleSmall = titleS.copy(fontSize = 14.sp, lineHeight = 19.sp),
    bodyLarge = body,
    bodyMedium = bodyS,
    bodySmall = meta,
    labelLarge = button,
    labelMedium = meta,
    labelSmall = eyebrow,
)
