package com.memoria.mobile.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The palette is stored as a bare string in DataStore and shared conceptually with
 * the web app, so an id this build does not know about is a normal situation — a
 * newer version's preference read by an older APK. It has to degrade to a working
 * theme, never to a crash on the very first frame.
 */
class ThemePresetTest {

    @Test
    fun `unknown ids fall back to the web default`() {
        assertEquals(ThemePreset.PASTEL_AZUL, ThemePreset.from(null))
        assertEquals(ThemePreset.PASTEL_AZUL, ThemePreset.from(""))
        assertEquals(ThemePreset.PASTEL_AZUL, ThemePreset.from("pastel-turquesa"))
    }

    @Test
    fun `ids round trip and are unique`() {
        val ids = ThemePreset.entries.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        ThemePreset.entries.forEach { assertEquals(it, ThemePreset.from(it.id)) }
    }

    @Test
    fun `every palette the web offers is present`() {
        // Verbatim from `getThemePalettes()` in the web app's app.js. A missing key
        // means an account whose theme cannot be represented on the phone.
        val expected = setOf(
            "default", "pastel-azul", "pastel-verde", "pastel-rosa", "pastel-lilas",
            "pastel-pessego", "pastel-menta", "pastel-areia", "pastel-coral",
        )
        assertEquals(expected, ThemePreset.entries.map { it.id }.toSet())
    }
}
