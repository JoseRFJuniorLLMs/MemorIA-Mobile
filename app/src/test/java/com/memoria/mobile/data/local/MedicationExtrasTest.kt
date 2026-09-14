package com.memoria.mobile.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `isEmpty` is what stops a cleared form from leaving a hollow record behind —
 * which the details screen would still treat as "has a treatment section" and
 * render as an empty card.
 */
class MedicationExtrasTest {

    private fun extras(
        days: Int? = null,
        continuous: Boolean = false,
        doctor: String = "",
        pharmacy: String = "",
    ) = MedicationExtras("m1", days, continuous, doctor, pharmacy)

    @Test
    fun `all blank counts as empty`() {
        assertTrue(extras().isEmpty)
    }

    @Test
    fun `any single filled field makes it non-empty`() {
        assertFalse(extras(days = 7).isEmpty)
        assertFalse(extras(continuous = true).isEmpty)
        assertFalse(extras(doctor = "Dr. João").isEmpty)
        assertFalse(extras(pharmacy = "Farmácia Central").isEmpty)
    }
}
