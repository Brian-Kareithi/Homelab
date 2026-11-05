package dev.kareithi.homelab.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.BuildConfig
import dev.kareithi.homelab.data.settings.AppSettings
import dev.kareithi.homelab.data.settings.Defaults
import dev.kareithi.homelab.data.settings.NotificationKind
import dev.kareithi.homelab.data.settings.ThemeMode
import dev.kareithi.homelab.domain.ConnectionKind
import dev.kareithi.homelab.ui.components.HomelabCard
import dev.kareithi.homelab.ui.components.HomelabScaffold
import dev.kareithi.homelab.ui.components.NavAction
import dev.kareithi.homelab.ui.components.SectionTitle
import dev.kareithi.homelab.ui.dashboard.DashboardViewModel

@Composable
fun SettingsScreen(
    vm: SettingsViewModel,
    dashboardVm: DashboardViewModel,
    onOpenMenu: () -> Unit,
) {
    val settings = vm.settings.collectAsStateWithLifecycle().value
    val tests by vm.tests.collectAsStateWithLifecycle()
    val dash by dashboardVm.state.collectAsState()

    HomelabScaffold(
        title = "Settings",
        navAction = NavAction.MENU,
        onNavAction = onOpenMenu,
    ) { padding ->
        if (settings == null) {
            Box(Modifier.fillMaxSize())
            return@HomelabScaffold
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() - 12.dp,
                bottom = padding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                SectionTitle("Server")
                HomelabCard(Modifier.fillMaxWidth()) {
                    ServerSettingsSection(
                        settings = settings,
                        tests = tests,
                        onModeChange = vm::setUrlMode,
                        onSave = vm::saveUrls,
                        onTest = vm::test,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            item {
                SectionTitle("Dashboard")
                HomelabCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TitleEditor(settings, vm::setTitle)
                        Text("Auto-refresh", style = MaterialTheme.typography.labelLarge)
                        RefreshSelector(settings.refreshSeconds, vm::setRefreshSeconds)
                    }
                }
            }
            item {
                SectionTitle("Appearance")
                HomelabCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ThemeSelector(settings.theme, vm::setTheme)
                        Text("Accent colour", style = MaterialTheme.typography.labelLarge)
                        AccentPicker(settings.accent, enabled = !settings.dynamicColor, onPick = vm::setAccent)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            SwitchRow(
                                title = "Dynamic colour",
                                subtitle = "Use your wallpaper colours instead of the Homepage palette",
                                checked = settings.dynamicColor,
                                onChange = vm::setDynamicColor,
                            )
                        }
                    }
                }
            }
            item {
                SectionTitle("Notifications")
                HomelabCard(Modifier.fillMaxWidth()) {
                    NotificationSettings(settings, vm::setNotification)
                }
            }
            item {
                SectionTitle("About")
                HomelabCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AboutLine("App", "Homelab ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        AboutLine("Homepage", dash.baseUrl ?: "—")
                        AboutLine(
                            "Connection",
                            when (dash.connection) {
                                ConnectionKind.LAN -> "LAN"
                                ConnectionKind.TAILSCALE -> "Tailscale"
                                ConnectionKind.CUSTOM -> "Custom"
                                ConnectionKind.OFFLINE -> "Offline"
                                null -> "—"
                            },
                        )
                        Text(
                            "All data comes from your Homepage instance. Service API keys stay on the server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun TitleEditor(settings: AppSettings, onSave: (String) -> Unit) {
    var title by remember(settings.title) { mutableStateOf(settings.title) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        TextButton(
            enabled = title != settings.title,
            onClick = { onSave(title.ifBlank { Defaults.TITLE }) },
        ) { Text("Save") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RefreshSelector(current: Int, onPick: (Int) -> Unit) {
    val options = Defaults.REFRESH_OPTIONS
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, s ->
            SegmentedButton(
                selected = current == s,
                onClick = { onPick(s) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                label = { Text(if (s == 0) "Off" else "${s}s", maxLines = 1) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(current: ThemeMode, onPick: (ThemeMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { i, mode ->
            SegmentedButton(
                selected = current == mode,
                onClick = { onPick(mode) },
                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
private fun AccentPicker(current: Long, enabled: Boolean, onPick: (Long) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Defaults.ACCENT_OPTIONS.forEach { argb ->
            val color = Color(argb)
            val selected = argb == current
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (enabled) color else color.copy(alpha = 0.35f))
                    .border(2.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                    .clickable(enabled = enabled) { onPick(argb) },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NotificationSettings(settings: AppSettings, onToggle: (NotificationKind, Boolean) -> Unit) {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<NotificationKind?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending?.let { if (granted) onToggle(it, true) }
        pending = null
    }
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(
            "Checked in the background every 15 minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        NotificationKind.entries.forEach { kind ->
            SwitchRow(
                title = kind.label,
                subtitle = kind.description,
                checked = kind in settings.notifications,
                modifier = Modifier.padding(horizontal = 16.dp),
                onChange = { enabled ->
                    val needsPermission = enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        pending = kind
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onToggle(kind, enabled)
                    }
                },
            )
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AboutLine(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
