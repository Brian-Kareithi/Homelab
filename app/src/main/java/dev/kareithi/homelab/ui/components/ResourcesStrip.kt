package dev.kareithi.homelab.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            gauges.forEach { GaugeCard(it, Modifier.weight(1f)) }
        }
    } else {
        Row(
            modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
    val target = (g.percent / 100.0).toFloat().coerceIn(0f, 1f)
    val sweep by animateFloatAsState(target, animationSpec = tween(700), label = "gauge")
    HomelabCard(modifier.widthIn(min = 96.dp)) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = c.statBlock,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(stroke),
                    )
                    // Sweep gradient starts at 3 o'clock; rotate so the arc grows from 12 o'clock.
                    rotate(-90f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0f to color.copy(alpha = 0.35f),
                                sweep.coerceAtLeast(0.01f) to color,
                                1f to color.copy(alpha = 0.35f),
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f * sweep,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Text(
                    Format.percent(g.percent),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                g.label.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                g.detail,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
