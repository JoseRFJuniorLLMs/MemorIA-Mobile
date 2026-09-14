package com.memoria.mobile.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * The consultation reminder is only useful if it fires BEFORE the appointment, and
 * if re-arming replaces the previous alarm instead of stacking a second copy.
 * Both are easy to break silently — a wrong request code shows up as duplicate
 * notifications, a wrong offset as a reminder nobody can act on.
 */
class ConsultationAlarmTest {

    private fun alarm(
        dateTime: String = "2026-09-20T09:30",
        lead: ConsultationAlarm.Lead = ConsultationAlarm.Lead.HOUR_BEFORE,
        id: String = "c1",
    ) = ConsultationAlarm(
        consultationId = id,
        professional = "Dra. Ana",
        location = "Clínica Central",
        dateTime = dateTime,
        lead = lead,
    )

    @Test
    fun `fires one hour before the appointment`() {
        assertEquals(
            LocalDateTime.parse("2026-09-20T08:30"),
            alarm(lead = ConsultationAlarm.Lead.HOUR_BEFORE).firesAt,
        )
    }

    @Test
    fun `fires a day before the appointment`() {
        assertEquals(
            LocalDateTime.parse("2026-09-19T09:30"),
            alarm(lead = ConsultationAlarm.Lead.DAY_BEFORE).firesAt,
        )
    }

    @Test
    fun `an unparseable date yields no trigger instead of throwing`() {
        val broken = alarm(dateTime = "ontem de manhã")
        assertNull(broken.firesAt)
        assertNull(broken.triggerAtMillis)
    }

    @Test
    fun `request code is stable per consultation and lead`() {
        // Stable across instances: re-arming must replace, not duplicate.
        assertEquals(alarm().requestCode, alarm().requestCode)
        // Distinct per lead, or the day-before alarm would cancel the hour-before.
        assertNotEquals(
            alarm(lead = ConsultationAlarm.Lead.DAY_BEFORE).requestCode,
            alarm(lead = ConsultationAlarm.Lead.HOUR_BEFORE).requestCode,
        )
        assertNotEquals(alarm(id = "c1").requestCode, alarm(id = "c2").requestCode)
    }

    @Test
    fun `lead phrases are distinct so the notification reads correctly`() {
        // The phrase is what the user actually reads ("Amanhã · 20/09 às 09:30").
        // Two leads sharing it would make the day-before reminder look like the
        // hour-before one.
        val phrases = ConsultationAlarm.Lead.entries.map { it.phrase }
        assertEquals(phrases.size, phrases.distinct().size)
    }
}
