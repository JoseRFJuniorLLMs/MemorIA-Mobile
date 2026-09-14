package com.memoria.mobile.ui.settings

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.memoria.mobile.data.ApiResult
import com.memoria.mobile.data.MemoriaRepository
import com.memoria.mobile.data.remote.User
import com.memoria.mobile.data.remote.VoiceAssistantConfig
import com.memoria.mobile.reminders.DoseAlarm
import com.memoria.mobile.reminders.MemoriaNotifications
import com.memoria.mobile.reminders.ReminderScheduler
import com.memoria.mobile.reminders.ReminderSound
import com.memoria.mobile.ui.theme.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class SettingsUiState(
    val loading: Boolean = true,
    val user: User? = null,
    val baseUrl: String = "",
    val error: String? = null,
    val message: String? = null,
    val checking: Boolean = false,
    val reachable: Boolean? = null,
    val hasSavedCredentials: Boolean = false,
    val notificationsAllowed: Boolean = false,
    val exactAlarmsAllowed: Boolean = true,
    val snoozeMinutes: Int = ReminderScheduler.DEFAULT_SNOOZE_MINUTES,
    val reminderSound: ReminderSound = ReminderSound.PADRAO,
    val lowStockAlerts: Boolean = true,
    val themePreset: String = ThemePreset.PASTEL_AZUL.id,
    val emergencyAlertMode: String = "auto",
    // Relatório automático por e-mail (guardado no servidor)
    val reportEnabled: Boolean = false,
    val reportFrequency: String = "weekly",
    val reportContactEmail: String = "",
    val reportSaving: Boolean = false,
    val lastReportSentAt: String? = null,
    // Assistente de voz (Alexa / Echo Dot)
    val voiceLoading: Boolean = false,
    val voiceSaving: Boolean = false,
    val voiceTesting: Boolean = false,
    val voiceEnabled: Boolean = false,
    val voiceProvider: String = "alexa",
    val voiceDeviceName: String = "",
    val voiceWebhookUrl: String = "",
) {
    val isPremium: Boolean get() = user?.isPremium == true
}

class SettingsViewModel(
    private val repo: MemoriaRepository,
    private val app: Application,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState(baseUrl = repo.currentBaseUrl()))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun load() {
        _state.value = _state.value.copy(loading = true, baseUrl = repo.currentBaseUrl())
        viewModelScope.launch {
            // Local preferences first: they resolve without the network, so sound,
            // theme and snooze stay usable even when the server cannot be reached.
            _state.value = _state.value.copy(
                hasSavedCredentials = repo.credentials.hasSaved(),
                snoozeMinutes = repo.snoozeMinutes(),
                reminderSound = ReminderSound.from(repo.reminderSound()),
                lowStockAlerts = repo.lowStockAlertsEnabled(),
                themePreset = repo.themePreset(),
                emergencyAlertMode = repo.emergencyAlertMode(),
                notificationsAllowed = MemoriaNotifications.canPost(app),
                exactAlarmsAllowed = scheduler.canScheduleExact(),
            )

            when (val r = repo.me()) {
                is ApiResult.Ok -> {
                    val report = r.value.reportSettings
                    _state.value = _state.value.copy(
                        loading = false,
                        user = r.value,
                        reportEnabled = report?.enabled ?: false,
                        reportFrequency = report?.frequency ?: "weekly",
                        reportContactEmail = report?.contactEmail?.ifBlank { r.value.email }
                            ?: r.value.email,
                        lastReportSentAt = report?.lastReportSentAt,
                    )
                    if (r.value.isPremium) loadVoiceAssistant()
                }
                is ApiResult.Err -> _state.value = _state.value.copy(
                    loading = false,
                    error = r.message,
                )
            }
        }
    }

    // ---- Lembretes ----

    fun setSnoozeMinutes(minutes: Int) {
        _state.value = _state.value.copy(snoozeMinutes = minutes)
        viewModelScope.launch {
            repo.setSnoozeMinutes(minutes)
            // Pending alarms carry the old snooze in their Intent, so re-arm.
            scheduler.reschedule()
            _state.value = _state.value.copy(message = "Soneca definida em $minutes minutos.")
        }
    }

    /**
     * Each sound profile is its own notification channel, so the armed alarms have
     * to be rebuilt: a pending Intent still points at the channel it was created
     * with, and changing the setting alone would keep the old tone forever.
     */
    fun setReminderSound(sound: ReminderSound) {
        _state.value = _state.value.copy(reminderSound = sound)
        viewModelScope.launch {
            repo.setReminderSound(sound.id)
            scheduler.reschedule()
            _state.value = _state.value.copy(message = "Som do lembrete: ${sound.label}.")
        }
    }

    fun setLowStockAlerts(enabled: Boolean) {
        _state.value = _state.value.copy(lowStockAlerts = enabled)
        viewModelScope.launch {
            repo.setLowStockAlertsEnabled(enabled)
            scheduler.reschedule()
            _state.value = _state.value.copy(
                message = if (enabled) {
                    "Vamos avisar quando um remédio estiver a acabar."
                } else {
                    "Avisos de estoque desligados."
                },
            )
        }
    }

    fun setThemePreset(preset: String) {
        _state.value = _state.value.copy(themePreset = preset)
        // No snackbar: the whole app repaints, which is feedback enough.
        viewModelScope.launch { repo.setThemePreset(preset) }
    }

    fun setEmergencyAlertMode(mode: String) {
        _state.value = _state.value.copy(emergencyAlertMode = mode)
        viewModelScope.launch { repo.setEmergencyAlertMode(mode) }
    }

    /**
     * Posts a reminder right now so the user can confirm — before a real dose is
     * at stake — that the alert breaks through their phone's settings, in the
     * sound profile they just picked.
     */
    fun sendTestNotification() {
        MemoriaNotifications.ensureChannels(app)
        if (!MemoriaNotifications.canPost(app)) {
            _state.value = _state.value.copy(
                notificationsAllowed = false,
                error = "As notificações estão bloqueadas. Abra as notificações do sistema e permita o MemorIA.",
            )
            return
        }
        val dose = DoseAlarm(
            medicationId = "teste",
            medicationName = "Teste do MemorIA",
            dosage = "Exemplo",
            instructions = "Se você está vendo isto, os lembretes funcionam.",
            time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
            date = LocalDate.now().toString(),
            snoozeMinutes = _state.value.snoozeMinutes,
            soundId = _state.value.reminderSound.id,
        )
        NotificationManagerCompat.from(app).notify(
            dose.requestCode,
            MemoriaNotifications.buildDoseNotification(app, dose, _state.value.reminderSound),
        )
        _state.value = _state.value.copy(message = "Lembrete de teste enviado.")
    }

    // ---- Relatório automático por e-mail ----

    fun onReportEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(reportEnabled = enabled)
        saveReportSettings()
    }

    fun onReportFrequency(frequency: String) {
        _state.value = _state.value.copy(reportFrequency = frequency)
        saveReportSettings()
    }

    fun onReportContactEmail(email: String) {
        _state.value = _state.value.copy(reportContactEmail = email)
    }

    /**
     * Saves the automated-report settings on the server — the switch behind the
     * whole weekly-report feature. With it on, the server mails the caregiver a
     * period summary of doses taken and missed, the health measurements logged on
     * the phone, and the consultations coming up.
     *
     * The address is validated here because the server answers a bad one with a
     * generic 400. An EMPTY address is allowed on purpose: it tells the server to
     * fall back to the account's own e-mail.
     */
    fun saveReportSettings() {
        val s = _state.value
        val email = s.reportContactEmail.trim()
        if (email.isNotBlank() && !email.matches(EMAIL)) {
            _state.value = s.copy(error = "E-mail de contato inválido.")
            return
        }
        _state.value = s.copy(reportSaving = true)
        viewModelScope.launch {
            when (
                val r = repo.setReportSettings(
                    enabled = s.reportEnabled,
                    frequency = s.reportFrequency,
                    contactEmail = email,
                )
            ) {
                is ApiResult.Ok -> {
                    val report = r.value.reportSettings
                    _state.value = _state.value.copy(
                        reportSaving = false,
                        user = r.value,
                        reportEnabled = report?.enabled ?: s.reportEnabled,
                        reportFrequency = report?.frequency ?: s.reportFrequency,
                        reportContactEmail = report?.contactEmail?.ifBlank { r.value.email } ?: email,
                        lastReportSentAt = report?.lastReportSentAt,
                        message = if (report?.enabled == true) {
                            "Relatório automático ligado."
                        } else {
                            "Relatório automático desligado."
                        },
                    )
                }
                is ApiResult.Err -> _state.value = _state.value.copy(
                    reportSaving = false,
                    error = r.message,
                )
            }
        }
    }

    // ---- Assistente de voz (Alexa / Echo Dot) ----

    private fun loadVoiceAssistant() {
        _state.value = _state.value.copy(voiceLoading = true)
        viewModelScope.launch {
            when (val r = repo.voiceAssistantConfig()) {
                is ApiResult.Ok -> _state.value = _state.value.copy(
                    voiceLoading = false,
                    voiceEnabled = r.value.enabled,
                    voiceProvider = r.value.provider,
                    voiceDeviceName = r.value.deviceName,
                    voiceWebhookUrl = r.value.webhookUrl,
                )
                // Silent on failure: an account that never set the integration up is
                // the normal case, and a banner here would read as something broken.
                is ApiResult.Err -> _state.value = _state.value.copy(voiceLoading = false)
            }
        }
    }

    fun onVoiceEnabled(enabled: Boolean) { _state.value = _state.value.copy(voiceEnabled = enabled) }
    fun onVoiceProvider(provider: String) { _state.value = _state.value.copy(voiceProvider = provider) }
    fun onVoiceDeviceName(name: String) { _state.value = _state.value.copy(voiceDeviceName = name) }
    fun onVoiceWebhookUrl(url: String) { _state.value = _state.value.copy(voiceWebhookUrl = url) }

    fun saveVoiceAssistant() {
        val s = _state.value
        // Checked before the request because the server's rejection is generic.
        // browser-tts is the one mode with no speaker for the server to post to.
        if (s.voiceEnabled && s.voiceProvider != "browser-tts" &&
            !s.voiceWebhookUrl.trim().startsWith("http")
        ) {
            _state.value = s.copy(
                error = "Informe a URL do webhook (http:// ou https://) para Alexa e outros assistentes.",
            )
            return
        }
        _state.value = s.copy(voiceSaving = true)
        viewModelScope.launch {
            val config = VoiceAssistantConfig(
                enabled = s.voiceEnabled,
                provider = s.voiceProvider,
                deviceName = s.voiceDeviceName.trim(),
                webhookUrl = s.voiceWebhookUrl.trim(),
            )
            when (val r = repo.setVoiceAssistantConfig(config)) {
                is ApiResult.Ok -> _state.value = _state.value.copy(
                    voiceSaving = false,
                    voiceEnabled = r.value.enabled,
                    voiceProvider = r.value.provider,
                    voiceDeviceName = r.value.deviceName,
                    voiceWebhookUrl = r.value.webhookUrl,
                    message = "Assistente de voz salvo.",
                )
                is ApiResult.Err -> _state.value = _state.value.copy(
                    voiceSaving = false,
                    error = r.message,
                )
            }
        }
    }

    /** Speaks one phrase on the Echo Dot so the user can hear it working. */
    fun testVoiceAssistant() {
        _state.value = _state.value.copy(voiceTesting = true)
        viewModelScope.launch {
            val result = repo.dispatchVoiceAssistant(
                message = "Teste do MemorIA: está na hora de tomar o seu remédio.",
                medicationName = "Teste",
                scheduleTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
            )
            _state.value = when (result) {
                is ApiResult.Ok -> _state.value.copy(
                    voiceTesting = false,
                    message = if (result.value.dispatched) {
                        "Mensagem enviada ao assistente."
                    } else {
                        // browser-tts has no speaker on the server side; saying so
                        // beats a success message the user will never hear.
                        "O provedor escolhido fala apenas no navegador, então nada foi enviado ao aparelho."
                    },
                )
                is ApiResult.Err -> _state.value.copy(voiceTesting = false, error = result.message)
            }
        }
    }

    // ---- Servidor / acesso ----

    fun onBaseUrl(v: String) { _state.value = _state.value.copy(baseUrl = v) }

    fun saveBaseUrl() {
        val url = _state.value.baseUrl.trim()
        if (url.isBlank()) { _state.value = _state.value.copy(error = "Informe a URL do servidor."); return }
        viewModelScope.launch {
            repo.setBaseUrl(url)
            _state.value = _state.value.copy(
                baseUrl = repo.currentBaseUrl(),
                message = "Servidor atualizado.",
                reachable = null,
            )
        }
    }

    fun testConnection() {
        _state.value = _state.value.copy(checking = true, reachable = null)
        viewModelScope.launch {
            // checkServer() carries the reason, so a failure names its cause
            // instead of only turning the indicator red.
            when (val r = repo.checkServer()) {
                is ApiResult.Ok ->
                    _state.value = _state.value.copy(checking = false, reachable = true)
                is ApiResult.Err ->
                    _state.value = _state.value.copy(checking = false, reachable = false, error = r.message)
            }
        }
    }

    /** Wipes the remembered CPF + password; the login form goes back to empty. */
    fun forgetCredentials() {
        viewModelScope.launch {
            repo.forgetCredentials()
            _state.value = _state.value.copy(
                hasSavedCredentials = false,
                message = "CPF e senha esquecidos neste celular.",
            )
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            repo.logout()
            onDone()
        }
    }

    fun consumeMessage() { _state.value = _state.value.copy(message = null) }
    fun clearError() { _state.value = _state.value.copy(error = null) }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
    }
}
