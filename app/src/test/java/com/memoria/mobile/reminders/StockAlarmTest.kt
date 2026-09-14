package com.memoria.mobile.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The stock warning's whole job is to be understandable at a glance — an elderly
 * user reading it on a lock screen has to know whether to go to the pharmacy
 * today. "0 unidades" and "restam 2, cerca de 1 dia" are different messages.
 */
class StockAlarmTest {

    private fun alarm(
        stock: Int,
        daysRemaining: Int,
        pharmacy: String = "",
        id: String = "m1",
        date: String = "2026-09-20",
    ) = StockAlarm(
        medicationId = id,
        medicationName = "Losartana",
        stock = stock,
        daysRemaining = daysRemaining,
        pharmacy = pharmacy,
        date = date,
    )

    @Test
    fun `an empty stock says so plainly`() {
        assertEquals("Sem unidades no estoque.", alarm(stock = 0, daysRemaining = 0).body)
    }

    @Test
    fun `stock that ends today is not reported as zero days left`() {
        // "cerca de 0 dias" reads like a bug; the wording names today instead.
        assertTrue(alarm(stock = 2, daysRemaining = 0).body.contains("hoje"))
    }

    @Test
    fun `remaining days are included when there are some`() {
        val body = alarm(stock = 6, daysRemaining = 3).body
        assertTrue(body.contains("6 unidade"))
        assertTrue(body.contains("3 dia"))
    }

    @Test
    fun `the pharmacy is named only when there is one`() {
        assertTrue(alarm(stock = 4, daysRemaining = 2, pharmacy = "Farmácia Central").body
            .contains("Farmácia: Farmácia Central"))
        assertTrue(!alarm(stock = 4, daysRemaining = 2).body.contains("Farmácia:"))
    }

    @Test
    fun `request code is once per medication per day`() {
        // Same day, same medication: re-arming replaces rather than piling up.
        assertEquals(alarm(1, 0).requestCode, alarm(1, 0).requestCode)
        assertNotEquals(alarm(1, 0, date = "2026-09-20").requestCode, alarm(1, 0, date = "2026-09-21").requestCode)
        assertNotEquals(alarm(1, 0, id = "m1").requestCode, alarm(1, 0, id = "m2").requestCode)
    }
}
