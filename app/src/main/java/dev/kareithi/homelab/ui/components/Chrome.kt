package dev.kareithi.homelab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.domain.ConnectionKind
import dev.kareithi.homelab.domain.Format
import dev.kareithi.homelab.ui.theme.HomelabTheme
import kotlinx.coroutines.delay

/** Current time in millis, ticking every [periodMs] so relative labels stay fresh. */
@Composable
fun rememberNow(periodMs: Long = 15_000L): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** Dot + label: LAN (green), Tailscale (blue), Custom (accent), Offline (red). */
@Composable
fun ConnectionIndicator(kind: ConnectionKind?, modifier: Modifier = Modifier) {
    val c = HomelabTheme.colors
    val (color, label) = when (kind) {
        ConnectionKind.LAN -> c.status.lan to "LAN"
        ConnectionKind.TAILSCALE -> c.status.tailscale to "Tailscale"
        ConnectionKind.CUSTOM -> c.accent to "Custom"
        ConnectionKind.OFFLINE -> c.status.error to "Offline"
        null -> MaterialTheme.colorScheme.onSurfaceVariant to "…"
    }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** "Offline · Last updated 3 min ago" banner. */
@Composable
fun OfflineBanner(lastUpdated: Long?, message: String?, now: Long, modifier: Modifier = Modifier) {
    val c = HomelabTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.status.warn.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = c.status.warn, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        val updated = lastUpdated?.let { "Last updated ${Format.relative(it, maxOf(now, it))}" } ?: "No data yet"
        Text(
            listOfNotNull("Offline", updated, message).joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = c.status.warn,
            maxLines = 2,
        )
    }
}
