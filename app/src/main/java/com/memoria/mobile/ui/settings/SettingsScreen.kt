package com.memoria.mobile.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memoria.mobile.reminders.ReminderSound
import com.memoria.mobile.ui.common.BackTopBar
import com.memoria.mobile.ui.common.SectionCard
import com.memoria.mobile.ui.common.systemViewModel
import com.memoria.mobile.ui.theme.GreenOk
import com.memoria.mobile.ui.theme.RedMiss
import com.memoria.mobile.ui.theme.ThemePreset
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * "Configurações" — the web `settingsPage`, section for section: reminder sound,
 * snooze, theme, automated e-mail report, emergency alert, LGPD, voice assistant,
 * plan, server and session.
 *
 * Two web sections are NOT repeated here because Android already owns them
 * properly: "Ativar Notificações" is the system permission (linked below), and
 * exporting or erasing data lives on the Privacy screen, which is where LGPD
 * expects it. Both are reachable from this screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onLoggedOut: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onBack: () -> Unit,
) {
    val vm = systemViewModel { repo, app, scheduler -> SettingsViewModel(repo, app, scheduler) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it); vm.clearError() }
    }

    Scaffold(
        topBar = { BackTopBar("Configurações", onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Conta", style = MaterialTheme.typography.titleLarge)
                    val user = state.user
                    if (user != null) {
                        Text(user.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        if (user.cpfMasked.isNotBlank()) Text("CPF: ${user.cpfMasked}", style = MaterialTheme.typography.bodyLarge)
                        if (user.email.isNotBlank()) Text(user.email, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (user.isPremium) "Plano: Premium" else "Plano: Gratuito",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else if (state.loading) {
                        Text("Carregando...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // ---- Lembretes: som, soneca, teste, permissões ----
            SectionCard("Lembretes", icon = Icons.Filled.NotificationsActive) {
                Text(
                    when {
                        !state.notificationsAllowed ->
                            "As notificações estão desligadas — nenhum lembrete de dose vai aparecer."
                        !state.exactAlarmsAllowed ->
                            "Os lembretes estão ligados, mas sem permissão de alarme exato podem " +
                                "atrasar alguns minutos."
                        else ->
                            "Lembretes ligados. O app avisa na hora de cada dose, mesmo com a tela bloqueada."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.notificationsAllowed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )

                Text("Som do lembrete", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderSound.entries.forEach { sound ->
                        FilterChip(
                            selected = state.reminderSound == sound,
                            onClick = { vm.setReminderSound(sound) },
                            label = { Text(sound.label) },
                        )
                    }
                }
                Text(
                    state.reminderSound.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    "Soneca do botão \"Adiar\": ${state.snoozeMinutes} minutos",
                    style = MaterialTheme.typography.bodyLarge,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { minutes ->
                        FilterChip(
                            selected = state.snoozeMinutes == minutes,
                            onClick = { vm.setSnoozeMinutes(minutes) },
                            label = { Text("$minutes min") },
                        )
                    }
                }

                OutlinedButton(onClick = vm::sendTestNotification, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                    Text("  Testar som e lembrete agora")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Abrir notificações do sistema")
                }
            }

            // ---- Estoque ----
            SectionCard("Estoque de medicamentos", icon = Icons.Filled.Inventory) {
                ToggleRow(
                    label = "Avisar quando um remédio estiver a acabar",
                    checked = state.lowStockAlerts,
                    onCheckedChange = vm::setLowStockAlerts,
                )
                Text(
                    "O aviso chega no seu celular às 9h. O servidor avisa também o cuidador " +
                        "pelo WhatsApp — e a farmácia, se você tiver cadastrado uma no medicamento.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---- Tema ----
            SectionCard(
                "Tema do Aplicativo",
                subtitle = "Escolha uma paleta em tons pastéis para melhor visualização.",
                icon = Icons.Filled.Palette,
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemePreset.entries.forEach { preset ->
                        ThemeSwatch(
                            preset = preset,
                            selected = state.themePreset == preset.id,
                            onClick = { vm.setThemePreset(preset.id) },
                        )
                    }
                }
                Text(
                    ThemePreset.from(state.themePreset).label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // ---- Relatório automático por e-mail ----
            SectionCard(
                "Envio Automático de Relatório por E-mail",
                subtitle = "O servidor envia ao cuidador um resumo do período: doses tomadas e " +
                    "perdidas, medições de saúde e consultas marcadas.",
                icon = Icons.AutoMirrored.Filled.Send,
            ) {
                ToggleRow(
                    label = if (state.reportEnabled) "Ativado" else "Desativado",
                    checked = state.reportEnabled,
                    enabled = !state.reportSaving,
                    onCheckedChange = vm::onReportEnabled,
                )

                Text("Frequência", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("weekly" to "Semanal", "monthly" to "Mensal").forEach { (value, label) ->
                        FilterChip(
                            selected = state.reportFrequency == value,
                            onClick = { vm.onReportFrequency(value) },
                            enabled = !state.reportSaving,
                            label = { Text(label) },
                        )
                    }
                }

                OutlinedTextField(
                    value = state.reportContactEmail,
                    onValueChange = vm::onReportContactEmail,
                    label = { Text("E-mail de contato") },
                    placeholder = { Text("Deixe vazio para usar o seu e-mail") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = vm::saveReportSettings,
                    enabled = !state.reportSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.reportSaving) {
                        CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
                    }
                    Text("Salvar relatório automático")
                }
                state.lastReportSentAt?.let {
                    Text(
                        "Último envio: ${formatInstant(it)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---- Alerta de emergência (Premium) ----
            SectionCard(
                "Alerta de Emergência (Premium)",
                subtitle = "Escolha se o contato de emergência é alertado automaticamente quando " +
                    "houver atraso no medicamento.",
                icon = Icons.Filled.WarningAmber,
            ) {
                if (!state.isPremium) {
                    PremiumLock(
                        "Alertar o filho ou cuidador automaticamente é um recurso do Premium.",
                        onOpenPlans,
                    )
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "auto" to "Automático imediato",
                            "confirm" to "Pedir confirmação",
                        ).forEach { (value, label) ->
                            FilterChip(
                                selected = state.emergencyAlertMode == value,
                                onClick = { vm.setEmergencyAlertMode(value) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            // ---- Assistente de voz (Alexa / Echo Dot) ----
            SectionCard(
                "Assistente de Voz (Alexa) — Premium",
                subtitle = "O lembrete também é falado no seu Echo Dot ou aparelho compatível.",
                icon = Icons.Filled.RecordVoiceOver,
            ) {
                if (!state.isPremium) {
                    PremiumLock(
                        "A integração com Alexa e Echo Dot é exclusiva do Premium.",
                        onOpenPlans,
                    )
                } else {
                    ToggleRow(
                        label = "Ativar integração de voz",
                        checked = state.voiceEnabled,
                        enabled = !state.voiceSaving && !state.voiceLoading,
                        onCheckedChange = vm::onVoiceEnabled,
                    )

                    Text("Provedor", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VOICE_PROVIDERS.forEach { (value, label) ->
                            FilterChip(
                                selected = state.voiceProvider == value,
                                onClick = { vm.onVoiceProvider(value) },
                                label = { Text(label) },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.voiceDeviceName,
                        onValueChange = vm::onVoiceDeviceName,
                        label = { Text("Nome do dispositivo") },
                        placeholder = { Text("Ex.: Echo da Sala") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.voiceWebhookUrl,
                        onValueChange = vm::onVoiceWebhookUrl,
                        label = { Text("URL do webhook") },
                        placeholder = { Text("https://seu-webhook.exemplo.com/alexa") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "O webhook é o endereço que fala no aparelho. Necessário para Alexa, " +
                            "Google e webhook; no modo \"Voz no navegador\" não é usado.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Button(
                        onClick = vm::saveVoiceAssistant,
                        enabled = !state.voiceSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.voiceSaving) {
                            CircularProgressIndicator(
                                Modifier.padding(end = 8.dp).size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                        Text("Salvar Alexa")
                    }
                    OutlinedButton(
                        onClick = vm::testVoiceAssistant,
                        enabled = !state.voiceTesting && state.voiceEnabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.voiceTesting) {
                            CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
                        }
                        Text("Falar um teste no aparelho")
                    }
                }
            }

            // ---- Privacidade / LGPD ----
            SectionCard(
                "Privacidade e LGPD",
                subtitle = "Consentimento, política de privacidade, exportar os seus dados e " +
                    "apagar a conta.",
                icon = Icons.Filled.Policy,
            ) {
                OutlinedButton(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth()) {
                    Text("Abrir privacidade e dados")
                }
            }

            // ---- Plano ----
            SectionCard("Plano", icon = Icons.Filled.WorkspacePremium) {
                OutlinedButton(onClick = onOpenPlans, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.isPremium) "Ver a minha assinatura" else "Inscreva-se no MemorIA")
                }
            }

            // ---- Servidor ----
            SectionCard(
                "Servidor (backend)",
                subtitle = "URL pública da API MemorIA. O app adiciona /api automaticamente.",
            ) {
                OutlinedTextField(
                    value = state.baseUrl,
                    onValueChange = vm::onBaseUrl,
                    label = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(onClick = vm::saveBaseUrl, modifier = Modifier.fillMaxWidth()) {
                    Text("Salvar servidor")
                }
                OutlinedButton(
                    onClick = vm::testConnection,
                    enabled = !state.checking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.checking) {
                        CircularProgressIndicator(Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
                    }
                    Text("Testar conexão")
                }
                when (state.reachable) {
                    true -> Text("Servidor online ✅", color = GreenOk, style = MaterialTheme.typography.bodyLarge)
                    false -> Text("Sem resposta do servidor ❌", color = RedMiss, style = MaterialTheme.typography.bodyLarge)
                    null -> {}
                }
            }

            // ---- Acesso ----
            SectionCard("Acesso") {
                Text(
                    if (state.hasSavedCredentials) {
                        "O CPF e a senha estão salvos neste celular, protegidos pelo cofre do " +
                            "Android. A tela de login já vem preenchida."
                    } else {
                        "O CPF e a senha não estão salvos. Marque \"Salvar meu CPF e senha\" " +
                            "na tela de login para não precisar digitar de novo."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Sits next to logout on purpose: handing the phone to someone
                // else is exactly when the saved password should go.
                OutlinedButton(
                    onClick = vm::forgetCredentials,
                    enabled = state.hasSavedCredentials,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.LockReset, contentDescription = null)
                    Text("  Esquecer CPF e senha salvos")
                }
            }

            Button(
                onClick = { vm.logout(onLoggedOut) },
                colors = ButtonDefaults.buttonColors(containerColor = RedMiss),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Text("  Sair da conta")
            }

            Text(
                "MemorIA Mobile 1.0.0 · não substitui orientação médica.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The four providers the backend accepts, in the same order as the web select. */
private val VOICE_PROVIDERS = listOf(
    "alexa" to "Alexa",
    "google" to "Google",
    "webhook" to "Webhook",
    "browser-tts" to "Voz no navegador",
)

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/**
 * A colour circle per palette, as the web renders its swatch row. The label is
 * carried by `contentDescription` because a colour alone says nothing to a screen
 * reader, and the selected palette's name is also printed under the row.
 */
@Composable
private fun ThemeSwatch(preset: ThemePreset, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = preset.primary,
        shape = CircleShape,
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
        } else {
            null
        },
        modifier = Modifier
            .size(52.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = preset.label },
    ) {
        if (selected) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
            }
        }
    }
}

/** The web's `.premium-lock-overlay`, as a line of text plus a way to subscribe. */
@Composable
private fun PremiumLock(message: String, onOpenPlans: () -> Unit) {
    Text(
        message,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onOpenPlans, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.WorkspacePremium, contentDescription = null)
        Text("  Ver planos")
    }
}

private val REPORT_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")

/** ISO instant to something readable; returns it unchanged when it does not parse. */
private fun formatInstant(raw: String): String = runCatching {
    OffsetDateTime.parse(raw).toLocalDateTime().format(REPORT_STAMP)
}.getOrDefault(raw)
