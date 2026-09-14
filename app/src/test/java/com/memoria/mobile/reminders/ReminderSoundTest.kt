package com.memoria.mobile.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Each sound profile MUST own a distinct channel id: Android freezes a channel's
 * sound and importance at creation, so two profiles sharing an id would make the
 * setting silently do nothing — the user would pick "Alto" and keep hearing the
 * quiet tone, with no error anywhere to explain it.
 */
class ReminderSoundTest {

    @Test
    fun `every profile has its own channel`() {
        val ids = ReminderSound.entries.map { it.channelId }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun `an unknown or missing id falls back to the default profile`() {
        // Stale preferences (an id written by another build) must not crash a screen.
        assertEquals(ReminderSound.PADRAO, ReminderSound.from(null))
        assertEquals(ReminderSound.PADRAO, ReminderSound.from(""))
        assertEquals(ReminderSound.PADRAO, ReminderSound.from("ultra-alto"))
    }

    @Test
    fun `ids round trip`() {
        ReminderSound.entries.forEach { assertEquals(it, ReminderSound.from(it.id)) }
    }

    @Test
    fun `the discreet profile is the only one without vibration`() {
        assertEquals(null, ReminderSound.SUAVE.vibrationPattern)
        assertNotEquals(null, ReminderSound.PADRAO.vibrationPattern)
        assertNotEquals(null, ReminderSound.ALTO.vibrationPattern)
    }
}
