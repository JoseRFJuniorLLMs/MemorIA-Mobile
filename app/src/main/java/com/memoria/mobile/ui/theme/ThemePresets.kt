package com.memoria.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The web app's "Tema do Aplicativo" palettes, value for value from
 * `getThemePalettes()` in `app.js`, so the phone and the browser look like the
 * same product for the same account.
 *
 * Only the accent family is themed. Background and text stay on the Material 3
 * surface roles: the pastel accents were chosen against a near-white page, and
 * painting the whole background with them would wreck the contrast the elderly
 * users this app is built for depend on — in dark mode especially.
 *
 * [dark] is a deepened accent rather than the web's `primaryDark`; a pastel tone
 * on a dark surface fails contrast, so the dark scheme leans on the darker end of
 * each family.
 */
enum class ThemePreset(
    val id: String,
    val label: String,
    val primary: Color,
    val dark: Color,
    val light: Color,
    val secondary: Color,
) {
    PADRAO(
        "default", "Padrão (Atual)",
        primary = Teal, dark = TealDark, light = TealLight, secondary = Amber,
    ),
    PASTEL_AZUL(
        "pastel-azul", "Pastel Azul",
        primary = Color(0xFF7AA2F7), dark = Color(0xFF2F4C86),
        light = Color(0xFFC7D8FB), secondary = Color(0xFF8ECAE6),
    ),
    PASTEL_VERDE(
        "pastel-verde", "Pastel Verde",
        primary = Color(0xFF7FC8A9), dark = Color(0xFF2F6350),
        light = Color(0xFFC6E8D9), secondary = Color(0xFFA8DDB5),
    ),
    PASTEL_ROSA(
        "pastel-rosa", "Pastel Rosa",
        primary = Color(0xFFE8A7C2), dark = Color(0xFF8A3F5E),
        light = Color(0xFFF6D6E3), secondary = Color(0xFFF3BFD3),
    ),
    PASTEL_LILAS(
        "pastel-lilas", "Pastel Lilás",
        primary = Color(0xFFB9A7E8), dark = Color(0xFF52407F),
        light = Color(0xFFDFD7F3), secondary = Color(0xFFD1C4F2),
    ),
    PASTEL_PESSEGO(
        "pastel-pessego", "Pastel Pêssego",
        primary = Color(0xFFF2B89B), dark = Color(0xFF8A4B2E),
        light = Color(0xFFF9DCCC), secondary = Color(0xFFFFD1B3),
    ),
    PASTEL_MENTA(
        "pastel-menta", "Pastel Menta",
        primary = Color(0xFF8FD3C1), dark = Color(0xFF2E6A5C),
        light = Color(0xFFD3ECE6), secondary = Color(0xFFB7E8DA),
    ),
    PASTEL_AREIA(
        "pastel-areia", "Pastel Areia",
        primary = Color(0xFFD8B98F), dark = Color(0xFF6F5333),
        light = Color(0xFFECDCC6), secondary = Color(0xFFEAD3AE),
    ),
    PASTEL_CORAL(
        "pastel-coral", "Pastel Coral",
        primary = Color(0xFFF29C9C), dark = Color(0xFF8C3535),
        light = Color(0xFFF8D3D3), secondary = Color(0xFFF7B7AA),
    );

    companion object {
        /** Unknown or missing id falls back to the web app's own default. */
        fun from(id: String?): ThemePreset =
            entries.firstOrNull { it.id == id } ?: PASTEL_AZUL
    }
}
