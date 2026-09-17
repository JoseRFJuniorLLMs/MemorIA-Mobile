package com.memoria.mobile.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.memoria.mobile.R
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.memoria.mobile.ui.common.repoViewModel
import com.memoria.mobile.ui.nav.Routes
import com.memoria.mobile.ui.theme.GreenOk
import com.memoria.mobile.ui.theme.RedMiss

private data class MoreEntry(
    val route: String,
    val label: String,
    val description: String,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
    val adminOnly: Boolean = false,
)

private val ENTRIES = listOf(
    MoreEntry(Routes.PLANS, "Inscreva-se no MemórIA", "Premium, benefícios e assinatura", iconRes = R.drawable.ic_menu_plans),
    MoreEntry(Routes.MEDS, "Medicamentos", "Seus remédios cadastrados", iconRes = R.drawable.ic_menu_medications),
    MoreEntry(Routes.CALENDAR, "Calendário", "Seus medicamentos dia a dia", iconRes = R.drawable.ic_menu_calendar),
    MoreEntry(Routes.REPORTS, "Relatórios", "Adesão, sequências e histórico", iconRes = R.drawable.ic_menu_reports),
    MoreEntry(Routes.REPLENISHMENT, "Reposição", "Quando comprar cada medicamento", iconRes = R.drawable.ic_menu_replenishment),
    MoreEntry(Routes.DOCTORS, "Meus Médicos", "Rede de cuidado e consultas", iconRes = R.drawable.ic_menu_doctors),
    MoreEntry(Routes.PRESCRIPTIONS, "Minhas Receitas", "Fotos das receitas médicas", iconRes = R.drawable.ic_menu_prescriptions),
    MoreEntry(Routes.PROFILE, "Meu Perfil", "Dados pessoais e contatos de emergência", iconVector = Icons.Filled.AccountCircle),
    MoreEntry(Routes.WHATSAPP, "WhatsApp", "Conexão e eventos de mensagens", iconVector = Icons.AutoMirrored.Filled.Chat),
    MoreEntry(Routes.SETTINGS, "Configurações", "Conta, servidor e sessão", iconVector = Icons.Filled.Settings),
    MoreEntry(Routes.OPTIMIZATION, "Otimização do App", "Bateria, alertas e tela bloqueada", iconVector = Icons.Filled.BatteryChargingFull),
    MoreEntry(Routes.PRIVACY, "Privacidade e dados", "Exportar; consentimento e apagar conta (LGPD)", iconVector = Icons.Filled.Shield),
    MoreEntry(Routes.HELP, "Ajuda e Tutorial", "Como usar o MemorIA", iconVector = Icons.AutoMirrored.Filled.HelpOutline),
    MoreEntry(Routes.ADMIN, "Painel do Proprietário", "Métricas do MemorIA", iconVector = Icons.Filled.TrendingUp, adminOnly = true),
)

/**
 * "Mais" — the web `moreMenu`. The admin entry only appears for an account the
 * server marks as admin, matching `admin-only-menu-item` on the web.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(contentPadding: PaddingValues, onNavigate: (String) -> Unit) {
    val vm = repoViewModel { MoreViewModel(it) }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Mais") }) }) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = contentPadding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.userName?.takeIf { it.isNotBlank() }?.let { name ->
                Text(name, style = MaterialTheme.typography.titleLarge)
                val statusText = when {
                    state.isTrialActive -> "Acesso Total (15 dias) · ${state.trialDaysRemaining ?: 15} dias restantes"
                    state.trialExpired -> "Período de 15 dias finalizado · Assine para ter acesso total"
                    state.isPremium -> "Plano Premium Ativo ✅"
                    else -> "Plano Gratuito"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        state.trialExpired -> RedMiss
                        state.isTrialActive -> GreenOk
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }

            ENTRIES.filter { !it.adminOnly || state.isAdmin }.forEach { entry ->
                MoreRow(entry) { onNavigate(entry.route) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreRow(entry: MoreEntry, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (entry.iconRes != null) {
                Image(
                    painter = painterResource(entry.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            } else if (entry.iconVector != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            entry.iconVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(entry.label, style = MaterialTheme.typography.titleLarge)
                Text(
                    entry.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
