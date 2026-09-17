package com.memoria.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun lightColorsFor(preset: ThemePreset) = lightColorScheme(
    // The accent is darkened for text and icon use: the pastel tones are chosen
    // to be gentle as a fill, and at their raw value they fail contrast against
    // white on the small labels this app leans on.
    primary = if (preset == ThemePreset.PADRAO) preset.primary else preset.dark,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = preset.light,
    onPrimaryContainer = preset.dark,
    secondary = preset.secondary,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White,
    error = RedMiss,
)

private fun darkColorsFor(preset: ThemePreset) = darkColorScheme(
    primary = preset.primary,
    onPrimary = preset.dark,
    primaryContainer = preset.dark,
    onPrimaryContainer = preset.light,
    secondary = preset.secondary,
    background = SurfaceDark,
    error = RedMiss,
)

// Slightly larger type baseline — accessibility for older users.
private val AppTypography = Typography(
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    bodyLarge = Typography().bodyLarge.copy(fontSize = 17.sp),
    labelLarge = Typography().labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
)

/**
 * [themePreset] is the palette id stored by Settings; an unknown one falls back
 * to the web app's default rather than throwing, so a preference written by a
 * newer build never bricks an older one.
 */
@Composable
fun MemoriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePreset: String? = null,
    content: @Composable () -> Unit,
) {
    val preset = ThemePreset.from(themePreset)
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorsFor(preset) else lightColorsFor(preset),
        typography = AppTypography,
        content = content,
    )
}
