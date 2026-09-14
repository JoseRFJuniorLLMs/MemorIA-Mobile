package com.memoria.mobile.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.memoria.mobile.MainActivity
import com.memoria.mobile.R

/**
 * The notification channels and the notifications themselves — doses,
 * consultations and low stock.
 *
 * The dose channels are IMPORTANCE_HIGH with sound and vibration on purpose:
 * this is a medication alert for an elderly user, so it has to break through — a
 * silent entry in the drawer is a missed dose. Android will not let the app
 * change a channel's sound or importance after creation, so every channel id
 * carries a version suffix and the user's sound choice selects between channels
 * ([ReminderSound]); a future change means a new id, not an edit that silently
 * does nothing.
 */
object MemoriaNotifications {

    const val CHANNEL_CONSULTATIONS = "memoria_consultas_v1"
    const val CHANNEL_STOCK = "memoria_estoque_v1"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val sound = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // One channel per sound profile: a channel's sound and importance cannot
        // be changed after creation, so "Som do lembrete" switches channels
        // rather than editing one. All three are created up front so the Android
        // notification settings show every option the app can use.
        ReminderSound.entries.forEach { profile ->
            manager.createNotificationChannel(
                NotificationChannel(
                    profile.channelId,
                    "Lembretes de medicamento — ${profile.label}",
                    profile.importance,
                ).apply {
                    description = profile.description
                    val pattern = profile.vibrationPattern
                    enableVibration(pattern != null)
                    if (pattern != null) vibrationPattern = pattern
                    setSound(profile.soundUri, profile.audioAttributes)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }

        val consultations = NotificationChannel(
            CHANNEL_CONSULTATIONS,
            "Consultas médicas",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisa antes de uma consulta marcada."
            setSound(sound, audio)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val stock = NotificationChannel(
            CHANNEL_STOCK,
            "Estoque de medicamentos",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisa quando um remédio está a acabar."
            setSound(sound, audio)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        manager.createNotificationChannel(consultations)
        manager.createNotificationChannel(stock)
    }

    /** False on Android 13+ until the user grants the runtime permission. */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Builds the dose reminder, with the same three answers the app itself
     * offers so the user never has to open it to record a dose.
     */
    fun buildDoseNotification(
        context: Context,
        dose: DoseAlarm,
        sound: ReminderSound = ReminderSound.PADRAO,
    ): Notification {
        val open = PendingIntent.getActivity(
            context,
            dose.requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            pendingFlags(),
        )

        return NotificationCompat.Builder(context, sound.channelId)
            .setSmallIcon(R.drawable.ic_stat_memoria)
            .setContentTitle("Hora do remédio: ${dose.medicationName}")
            .setContentText(doseBody(dose))
            .setStyle(NotificationCompat.BigTextStyle().bigText(doseBody(dose)))
            // Pre-O phones have no channels, so the profile has to be applied to
            // the notification itself for the setting to mean anything there.
            .setPriority(
                if (sound == ReminderSound.SUAVE) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH,
            )
            .setSound(sound.soundUri)
            .also { builder ->
                sound.vibrationPattern?.let { builder.setVibrate(it) }
            }
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            // The dose stays on screen until answered: auto-dismissing a
            // medication alert after a few seconds defeats the whole point.
            .setOngoing(false)
            .setContentIntent(open)
            .addAction(
                0,
                "Tomei",
                actionIntent(context, ReminderReceiver.ACTION_TAKEN, dose),
            )
            .addAction(
                0,
                "Adiar ${dose.snoozeMinutes} min",
                actionIntent(context, ReminderReceiver.ACTION_SNOOZE, dose),
            )
            .addAction(
                0,
                "Não tomei",
                actionIntent(context, ReminderReceiver.ACTION_MISSED, dose),
            )
            .build()
    }

    fun buildConsultationNotification(
        context: Context,
        professional: String,
        whenLabel: String,
        location: String,
        requestCode: Int,
    ): Notification {
        val open = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            pendingFlags(),
        )
        val body = listOfNotNull(whenLabel, location.takeIf { it.isNotBlank() }).joinToString(" · ")
        return NotificationCompat.Builder(context, CHANNEL_CONSULTATIONS)
            .setSmallIcon(R.drawable.ic_stat_memoria)
            .setContentTitle("Consulta: $professional")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
    }

    /**
     * "This medicine is running out" — the phone-side half of the stock alert.
     *
     * The server already messages the caregiver (and the preferred pharmacy) over
     * WhatsApp when the stock goes critical; this is what the patient themselves
     * sees, so buying more does not depend on someone else reading a message.
     */
    fun buildLowStockNotification(context: Context, alarm: StockAlarm): Notification {
        val open = PendingIntent.getActivity(
            context,
            alarm.requestCode,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            pendingFlags(),
        )
        return NotificationCompat.Builder(context, CHANNEL_STOCK)
            .setSmallIcon(R.drawable.ic_stat_memoria)
            .setContentTitle(
                if (alarm.stock <= 0) "Acabou: ${alarm.medicationName}"
                else "Está a acabar: ${alarm.medicationName}",
            )
            .setContentText(alarm.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alarm.body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
    }

    private fun doseBody(dose: DoseAlarm): String = listOfNotNull(
        dose.dosage.takeIf { it.isNotBlank() },
        "Marcado para ${dose.time}",
        dose.instructions?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    private fun actionIntent(context: Context, action: String, dose: DoseAlarm): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            // Distinct request codes per action, otherwise the three buttons
            // would collapse onto one PendingIntent and all do the same thing.
            dose.requestCode + action.hashCode(),
            ReminderReceiver.intentFor(context, action, dose),
            pendingFlags(),
        )

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
