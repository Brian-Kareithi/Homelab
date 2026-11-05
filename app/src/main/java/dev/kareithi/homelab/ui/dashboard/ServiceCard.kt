package dev.kareithi.homelab.ui.dashboard

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.kareithi.homelab.domain.DockerState
import dev.kareithi.homelab.domain.HealthCheck
import dev.kareithi.homelab.domain.ServiceUi
import dev.kareithi.homelab.domain.WidgetState
import dev.kareithi.homelab.ui.components.CardShape
import dev.kareithi.homelab.ui.components.ErrorLine
import dev.kareithi.homelab.ui.components.HealthPill
import dev.kareithi.homelab.ui.components.HomelabCard
import dev.kareithi.homelab.ui.components.ServiceIcon
import dev.kareithi.homelab.ui.components.ShimmerStatRow
import dev.kareithi.homelab.ui.components.StatRow
import dev.kareithi.homelab.ui.components.StatusPill

/** How many stat blocks a widget type shows, for the loading shimmer. */
private fun expectedStats(type: String?): Int = when (type?.lowercase()) {
    "sonarr", "lidarr" -> 3
    else -> 4
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServiceCard(
    service: ServiceUi,
    widget: WidgetState?,
    docker: DockerState?,
    health: HealthCheck?,
    baseUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    HomelabCard(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ServiceIcon(service.icon, service.name, size = 40.dp, baseUrl = baseUrl)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(service.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    service.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    docker?.let { StatusPill(it.label, it.level) }
                    health?.let { HealthPill(it) }
                }
            }
            if (widget != null) {
                val data = widget.data
                when {
                    data != null && data.stats.isNotEmpty() -> {
                        Spacer(Modifier.height(10.dp))
                        StatRow(data.stats)
                    }
                    data == null && widget.loading -> {
                        Spacer(Modifier.height(10.dp))
                        ShimmerStatRow(expectedStats(service.widget?.type))
                    }
                }
                widget.error?.let {
                    Spacer(Modifier.height(6.dp))
                    ErrorLine(it)
                }
            }
        }
    }
}
