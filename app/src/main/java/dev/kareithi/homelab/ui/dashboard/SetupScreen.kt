package dev.kareithi.homelab.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.ui.components.ErrorLine
import dev.kareithi.homelab.ui.components.HomelabCard
import dev.kareithi.homelab.ui.settings.ServerSettingsSection
import dev.kareithi.homelab.ui.settings.SettingsViewModel
import dev.kareithi.homelab.ui.theme.HomelabTheme

/** Shown on first launch when nothing is cached and Homepage can't be reached. */
@Composable
fun SetupScreen(
    dashboardState: DashboardUiState,
    settingsVm: SettingsViewModel,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = settingsVm.settings.collectAsStateWithLifecycle().value
    val tests by settingsVm.tests.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.size(8.dp))
        Icon(Icons.Filled.Dns, contentDescription = null, tint = HomelabTheme.colors.accent, modifier = Modifier.size(56.dp))
        Text("Connect to your Homepage", style = MaterialTheme.typography.titleLarge)
        Text(
            "Homelab reads everything from your Homepage server. Make sure you're on home Wi-Fi " +
                "or Tailscale is connected, then check the address below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        dashboardState.offlineMessage?.let { ErrorLine(it) }

        if (settings != null) {
            HomelabCard(Modifier.fillMaxWidth().widthIn(max = 560.dp)) {
                ServerSettingsSection(
                    settings = settings,
                    tests = tests,
                    onModeChange = settingsVm::setUrlMode,
                    onSave = settingsVm::saveUrls,
                    onTest = settingsVm::test,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        Button(onClick = onRetry, enabled = !dashboardState.isRefreshing) {
            if (dashboardState.isRefreshing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Connecting…")
                }
            } else {
                Text("Retry")
            }
        }
    }
}
