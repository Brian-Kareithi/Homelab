package dev.kareithi.homelab.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.domain.Format
import dev.kareithi.homelab.domain.Resources
import dev.kareithi.homelab.ui.theme.HomelabTheme
import java.util.Locale

private data class Gauge(val label: String, val percent: Double, val detail: String)

/** CPU / RAM / Disk gauges from Homepage's resources info widget. */
@Composable
fun ResourcesStrip(resources: Resources, modifier: Modifier = Modifier) {
    val gauges = buildList {
        resources.cpu?.let {
            add(Gauge("CPU", it.usagePercent, String.format(Locale.US, "load %.2f", it.load)))
        }
        resources.memory?.let {
            add(Gauge("RAM", it.percent, "${gb(it.usedBytes)} / ${gb(it.totalBytes)} GB"))
        }
        resources.disks.forEach {
            add(
                Gauge(
                    label = if (resources.disks.size > 1) it.target else "Disk",
                    percent = it.usePercent,
                    detail = "${tb(it.usedBytes)} / ${tb(it.sizeBytes)} TB",
                ),
            )
        }
    }
    if (gauges.isEmpty()) return

    if (gauges.size <= 3) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            gauges.forEach { GaugeCard(it, Modifier.weight(1f)) }
        }
    } else {
        Row(
            modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            gauges.forEach { GaugeCard(it, Modifier.width(120.dp)) }
        }
    }
}

private fun gb(bytes: Double): String = String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024 * 1024))

private fun tb(bytes: Double): String {
    val t = bytes / (1024.0 * 1024 * 1024 * 1024)
    return String.format(Locale.US, if (t < 10) "%.2f" else "%.1f", t)
}

@Composable
private fun GaugeCard(g: Gauge, modifier: Modifier = Modifier) {
    val c = HomelabTheme.colors
    val color: Color = when {
        g.percent >= 90 -> c.status.error
        g.percent >= 80 -> c.status.warn
        else -> c.accent
    }
    HomelabCard(modifier.widthIn(min = 96.dp)) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (g.percent / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.size(64.dp),
                    color = color,
                    trackColor = c.statBlock,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Text(Format.percent(g.percent), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(g.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                g.detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
