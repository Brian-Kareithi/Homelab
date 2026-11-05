package dev.kareithi.homelab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.data.net.ConnectionTestResult
import dev.kareithi.homelab.data.settings.AppSettings
import dev.kareithi.homelab.data.settings.Defaults
import dev.kareithi.homelab.data.settings.UrlMode
import dev.kareithi.homelab.ui.theme.HomelabTheme

/**
 * Base URL mode + editable URLs + "Test connection". Shared by Settings and the first-launch
 * setup screen. [onSaved] runs after Save (e.g. to retry loading).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsSection(
    settings: AppSettings,
    tests: Map<UrlField, TestState>,
    onModeChange: (UrlMode) -> Unit,
    onSave: (lan: String, tailscale: String, custom: String) -> Unit,
    onTest: (mode: UrlMode, lan: String, tailscale: String, custom: String) -> Unit,
    modifier: Modifier = Modifier,
    onSaved: () -> Unit = {},
) {
    var lan by remember(settings.lanUrl) { mutableStateOf(settings.lanUrl) }
    var tailscale by remember(settings.tailscaleUrl) { mutableStateOf(settings.tailscaleUrl) }
    var custom by remember(settings.customUrl) { mutableStateOf(settings.customUrl) }
    val dirty = lan != settings.lanUrl || tailscale != settings.tailscaleUrl || custom != settings.customUrl

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            UrlMode.entries.forEachIndexed { i, mode ->
                SegmentedButton(
                    selected = settings.urlMode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(i, UrlMode.entries.size),
                    label = { Text(mode.label, maxLines = 1) },
                )
            }
        }
        Text(
            when (settings.urlMode) {
                UrlMode.AUTO -> "Tries the LAN address first (1.5 s), then falls back to Tailscale."
                UrlMode.LAN -> "Always use the home Wi-Fi address."
                UrlMode.TAILSCALE -> "Always use the Tailscale address (works anywhere with Tailscale on)."
                UrlMode.CUSTOM -> "Any Homepage URL, e.g. MagicDNS ${Defaults.MAGIC_DNS_URL}."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        UrlTextField("LAN URL", lan, { lan = it }, tests[UrlField.LAN])
        UrlTextField("Tailscale URL", tailscale, { tailscale = it }, tests[UrlField.TAILSCALE])
        UrlTextField("Custom URL", custom, { custom = it }, tests[UrlField.CUSTOM])

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onTest(settings.urlMode, lan, tailscale, custom) }) {
                Text("Test connection")
            }
            Button(
                enabled = dirty,
                onClick = {
                    onSave(lan, tailscale, custom)
                    onSaved()
                },
            ) { Text("Save") }
        }
    }
}

@Composable
private fun UrlTextField(label: String, value: String, onChange: (String) -> Unit, test: TestState?) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        when (test) {
            null -> Unit
            TestState.Running -> Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(6.dp))
                Text("Testing…", style = MaterialTheme.typography.labelSmall)
            }
            is TestState.Done -> {
                val c = HomelabTheme.colors
                val status = c.status
                Spacer(Modifier.height(4.dp))
                when (val r = test.result) {
                    is ConnectionTestResult.Success -> Text(
                        "Connected · ${r.groups} groups, ${r.services} services",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textOn(status.ok, c.card),
                    )
                    is ConnectionTestResult.Failure -> Text(
                        "Failed · ${r.message}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textOn(status.error, c.card),
                    )
                }
            }
        }
    }
}
