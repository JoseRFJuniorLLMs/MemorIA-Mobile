package com.memoria.mobile.reminders

import android.content.Intent
import kotlin.math.absoluteValue

/**
 * One "this medicine is running out" reminder, flattened for the alarm Intent.
 *
 * The stock figure and the days of coverage are computed when the alarm is armed,
 * not when it fires: the receiver then needs no network, which is the same
 * reasoning as [DoseAlarm]. A stock that changes in the meantime re-arms the
 * window anyway, because saving a medication triggers a reschedule.
 */
data class StockAlarm(
    val medicationId: String,
    val medicationName: String,
    val stock: Int,
    /** Whole days of coverage left at the current pace; -1 when unknown. */
    val daysRemaining: Int,
    /** Pharmacy the user chose for this medication, for the notification text. */
    val pharmacy: String,
    /** Local date the alarm belongs to, ISO `yyyy-MM-dd` — keeps it once a day. */
    val date: String,
) {
    val requestCode: Int
        get() = "estoque|$medicationId|$date".hashCode().absoluteValue

    val body: String
        get() = buildList {
            add(
                when {
                    stock <= 0 -> "Sem unidades no estoque."
                    daysRemaining <= 0 -> "Restam $stock unidade(s) — deve acabar hoje."
                    else -> "Restam $stock unidade(s), cerca de $daysRemaining dia(s)."
                }
            )
            if (pharmacy.isNotBlank()) add("Farmácia: $pharmacy")
        }.joinToString(" ")

    fun writeTo(intent: Intent): Intent = intent.apply {
        putExtra(EXTRA_MED_ID, medicationId)
        putExtra(EXTRA_MED_NAME, medicationName)
        putExtra(EXTRA_STOCK, stock)
        putExtra(EXTRA_DAYS, daysRemaining)
        putExtra(EXTRA_PHARMACY, pharmacy)
        putExtra(EXTRA_DATE, date)
    }

    companion object {
        private const val EXTRA_MED_ID = "stock_med_id"
        private const val EXTRA_MED_NAME = "stock_med_name"
        private const val EXTRA_STOCK = "stock_units"
        private const val EXTRA_DAYS = "stock_days"
        private const val EXTRA_PHARMACY = "stock_pharmacy"
        private const val EXTRA_DATE = "stock_date"

        fun readFrom(intent: Intent): StockAlarm? {
            val id = intent.getStringExtra(EXTRA_MED_ID) ?: return null
            val date = intent.getStringExtra(EXTRA_DATE) ?: return null
            return StockAlarm(
                medicationId = id,
                medicationName = intent.getStringExtra(EXTRA_MED_NAME).orEmpty(),
                stock = intent.getIntExtra(EXTRA_STOCK, 0),
                daysRemaining = intent.getIntExtra(EXTRA_DAYS, -1),
                pharmacy = intent.getStringExtra(EXTRA_PHARMACY).orEmpty(),
                date = date,
            )
        }
    }
}
