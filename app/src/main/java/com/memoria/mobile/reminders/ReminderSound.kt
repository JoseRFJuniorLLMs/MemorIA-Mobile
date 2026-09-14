package com.memoria.mobile.reminders

import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings

/**
 * The web app's "Som de Notificação" (Padrão / Suave / Alto), expressed the only
 * way Android allows.
 *
 * A channel's sound, vibration and importance are frozen the moment the channel
 * is created — `createNotificationChannel` on an existing id silently ignores
 * them. So each profile is its OWN channel, and switching the setting switches
 * which channel the dose reminder is posted to. Editing a profile later means a
 * new [channelId] suffix, never an edit in place that appears to work and does
 * nothing.
 *
 * `ALTO` deliberately borrows the alarm tone: for someone hard of hearing, the
 * default notification blip is the difference between a taken and a missed dose.
 */
enum class ReminderSound(
    val id: String,
    val label: String,
    val description: String,
    val channelId: String,
    val importance: Int,
    private val vibration: LongArray?,
) {
    PADRAO(
        id = "default",
        label = "Padrão",
        description = "Som de notificação do telefone, com vibração.",
        channelId = "memoria_doses_v1",
        importance = NotificationManager.IMPORTANCE_HIGH,
        vibration = longArrayOf(0, 400, 250, 400),
    ),
    SUAVE(
        id = "soft",
        label = "Suave",
        description = "Aviso discreto, sem vibração e sem aparecer sobre a tela.",
        channelId = "memoria_doses_suave_v1",
        importance = NotificationManager.IMPORTANCE_DEFAULT,
        vibration = null,
    ),
    ALTO(
        id = "loud",
        label = "Alto",
        description = "Toque de despertador e vibração longa — para quem ouve pouco.",
        channelId = "memoria_doses_alto_v1",
        importance = NotificationManager.IMPORTANCE_HIGH,
        vibration = longArrayOf(0, 800, 300, 800, 300, 800),
    );

    val soundUri: Uri
        get() = if (this == ALTO) {
            Settings.System.DEFAULT_ALARM_ALERT_URI ?: Settings.System.DEFAULT_NOTIFICATION_URI
        } else {
            Settings.System.DEFAULT_NOTIFICATION_URI
        }

    val audioAttributes: AudioAttributes
        get() = AudioAttributes.Builder()
            .setUsage(
                if (this == ALTO) AudioAttributes.USAGE_ALARM
                else AudioAttributes.USAGE_NOTIFICATION_EVENT,
            )
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    val vibrationPattern: LongArray? get() = vibration?.copyOf()

    companion object {
        /** Falls back to [PADRAO] for an unknown id, never throws on stale prefs. */
        fun from(id: String?): ReminderSound =
            entries.firstOrNull { it.id == id } ?: PADRAO
    }
}
