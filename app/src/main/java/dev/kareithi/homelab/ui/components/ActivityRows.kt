package dev.kareithi.homelab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.domain.DetailRow
import dev.kareithi.homelab.domain.DetailSection
import dev.kareithi.homelab.domain.Format
import dev.kareithi.homelab.domain.NowPlaying
import dev.kareithi.homelab.domain.QueueItem
import dev.kareithi.homelab.domain.TorrentItem
import dev.kareithi.homelab.ui.theme.HomelabTheme
import java.time.Instant
import java.util.Locale

@Composable
fun ThinProgress(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(4.dp),
        color = HomelabTheme.colors.accent,
        trackColor = HomelabTheme.colors.statBlock,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
private fun Secondary(text: String, modifier: Modifier = Modifier, maxLines: Int = 1) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
fun NowPlayingRow(item: NowPlaying, modifier: Modifier = Modifier) {
    HomelabCard(modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (item.isPaused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (item.isPaused) "Paused" else "Playing",
                tint = if (item.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else HomelabTheme.colors.accent,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                item.subtitle?.let { Secondary(it) }
                Secondary(listOfNotNull(item.user, item.device).joinToString(" · "))
                Spacer(Modifier.height(6.dp))
                ThinProgress(item.progress)
                if (item.runtimeSeconds > 0) {
                    Spacer(Modifier.height(2.dp))
                    Secondary("${Format.duration(item.positionSeconds)} / ${Format.duration(item.runtimeSeconds)}")
                }
            }
        }
    }
}

@Composable
fun QueueRow(item: QueueItem, now: Instant, modifier: Modifier = Modifier) {
    HomelabCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            val eta = Format.parseInstant(item.estimatedCompletion)?.let { Format.relative(it, now) }
            Secondary(
                listOfNotNull(
                    item.source.takeIf { it.isNotBlank() },
                    item.status.replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    item.sizeBytes.takeIf { it > 0 }?.let { Format.bytes(it) },
                    eta,
                ).joinToString(" · "),
            )
            item.progress?.let {
                Spacer(Modifier.height(6.dp))
                ThinProgress(it)
            }
        }
    }
}

@Composable
fun TorrentRow(item: TorrentItem, modifier: Modifier = Modifier) {
    HomelabCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            ThinProgress(item.progress)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Secondary(
                    listOfNotNull(
                        Format.percent(item.progress * 100.0),
                        item.state,
                        Format.bytes(item.sizeBytes),
                        if (!item.isComplete) "ETA ${Format.eta(item.etaSeconds)}" else null,
                        item.category,
                    ).joinToString(" · "),
                    Modifier.weight(1f),
                )
                SpeedLabel(down = true, bytesPerSecond = item.downloadSpeed)
                Spacer(Modifier.width(8.dp))
                SpeedLabel(down = false, bytesPerSecond = item.uploadSpeed)
            }
        }
    }
}

@Composable
private fun SpeedLabel(down: Boolean, bytesPerSecond: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (down) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
            contentDescription = if (down) "Download" else "Upload",
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(Format.speed(bytesPerSecond), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Header row for a [DetailSection] rendered as a table. */
@Composable
fun DetailTableHeader(section: DetailSection, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text("Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
        section.headers.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** One row of a [DetailSection]: either a list item or a table row. */
@Composable
fun DetailRowView(row: DetailRow, isTable: Boolean, modifier: Modifier = Modifier) {
    if (isTable) {
        Row(
            modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(row.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(2f))
            row.cells.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    } else {
        Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(row.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            row.subtitle?.let { Secondary(it) }
            row.progress?.let {
                Spacer(Modifier.height(4.dp))
                ThinProgress(it)
            }
        }
    }
}
