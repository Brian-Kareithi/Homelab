package dev.kareithi.homelab.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.domain.DetailSection
import dev.kareithi.homelab.domain.DockerState
import dev.kareithi.homelab.domain.DockerStats
import dev.kareithi.homelab.domain.Format
import dev.kareithi.homelab.domain.HealthCheck
import dev.kareithi.homelab.domain.ServiceUi
import dev.kareithi.homelab.domain.Stat
import dev.kareithi.homelab.domain.WidgetMappers
import dev.kareithi.homelab.ui.components.AutoRefreshEffect
import dev.kareithi.homelab.ui.components.DetailRowView
import dev.kareithi.homelab.ui.components.DetailTableHeader
import dev.kareithi.homelab.ui.components.EmptyHint
import dev.kareithi.homelab.ui.components.GroupHeaderSkeleton
import dev.kareithi.homelab.ui.components.ListRowSkeleton
import dev.kareithi.homelab.ui.components.ErrorLine
import dev.kareithi.homelab.ui.components.HealthPill
import dev.kareithi.homelab.ui.components.HomelabCard
import dev.kareithi.homelab.ui.components.HomelabScaffold
import dev.kareithi.homelab.ui.components.NavAction
import dev.kareithi.homelab.ui.components.NowPlayingRow
import dev.kareithi.homelab.ui.components.QueueRow
import dev.kareithi.homelab.ui.components.SectionTitle
import dev.kareithi.homelab.ui.components.ServiceIcon
import dev.kareithi.homelab.ui.components.ShimmerStatRow
import dev.kareithi.homelab.ui.components.StatRow
import dev.kareithi.homelab.ui.components.StatusPill
import dev.kareithi.homelab.ui.components.TorrentRow
import dev.kareithi.homelab.ui.components.rememberNow
import dev.kareithi.homelab.ui.components.rememberUrlOpener
import dev.kareithi.homelab.ui.dashboard.DashboardViewModel
import dev.kareithi.homelab.ui.theme.HomelabTheme
import java.time.Instant
import java.util.Locale

@Composable
fun ServiceDetailScreen(
    serviceKey: String,
    vm: DashboardViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val openUrl = rememberUrlOpener()
    val now = Instant.ofEpochMilli(rememberNow())
    val service = state.service(serviceKey)

    AutoRefreshEffect(settings.refreshSeconds) { vm.autoRefresh() }

    HomelabScaffold(
        title = service?.name ?: "Service",
        subtitle = service?.group,
        navAction = NavAction.BACK,
        onNavAction = onBack,
        isRefreshing = state.isUserRefreshing,
        onRefresh = { vm.refresh(userInitiated = true) },
    ) { padding ->
        if (service == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                EmptyHint("This service is no longer in Homepage's configuration.")
            }
            return@HomelabScaffold
        }
        val widget = state.widgets[service.key]
        val data = widget?.data

        run {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "header") {
                    Header(service, state.baseUrl, state.docker[service.key], state.health[service.key]) {
                        service.href?.let(openUrl)
                    }
                }

                val dockerStats = state.dockerStats[service.key]
                if (dockerStats != null) {
                    item(key = "docker") {
                        SectionTitle("Container")
                        DockerStatsCard(service, state.docker[service.key], dockerStats)
                    }
                } else if (service.hasDocker && state.isRefreshing) {
                    item(key = "docker:skeleton") {
                        SectionTitle("Container")
                        ShimmerStatRow(4)
                    }
                }

                if (service.widget != null) {
                    val supported = WidgetMappers.isSupported(service.widget.type)
                    item(key = "stats") {
                        SectionTitle(service.widget.type.replaceFirstChar { it.titlecase(Locale.getDefault()) })
                        when {
                            !supported -> EmptyHint("The ${service.widget.type} widget isn't supported in the app yet.")
                            data != null && data.stats.isNotEmpty() -> StatRow(data.stats)
                            widget?.loading == true -> ShimmerStatRow(4)
                        }
                        widget?.error?.let {
                            Spacer(Modifier.height(6.dp))
                            ErrorLine(it)
                        }
                    }
                }

                if (data == null && widget?.loading == true) {
                    item(key = "sk:title") { GroupHeaderSkeleton() }
                    items(3, key = { "sk:$it" }) { ListRowSkeleton(leading = false, progress = it == 0) }
                }

                if (data != null) {
                    if (data.sessions.isNotEmpty() || service.widget?.type.equals("jellyfin", true)) {
                        item(key = "h:sessions") { SectionTitle("Now playing", trailing = data.sessions.size.toString()) }
                        if (data.sessions.isEmpty()) item(key = "e:sessions") { EmptyHint("Nothing is playing.") }
                        items(data.sessions.size, key = { "s:$it" }) { i -> NowPlayingRow(data.sessions[i]) }
                    }
                    if (data.queue.isNotEmpty() || service.widget?.type.equals("sonarr", true)) {
                        item(key = "h:queue") {
                            val total = data.queueTotal ?: data.queue.size.toLong()
                            SectionTitle(
                                "Queue",
                                trailing = if (total > data.queue.size) "${data.queue.size} of $total" else total.toString(),
                            )
                        }
                        if (data.queue.isEmpty()) item(key = "e:queue") { EmptyHint("Queue is empty.") }
                        items(data.queue.size, key = { "q:$it" }) { i -> QueueRow(data.queue[i], now) }
                    }
                    if (data.torrents.isNotEmpty() || service.widget?.type.equals("qbittorrent", true)) {
                        val active = data.torrents.filter { it.isActive }
                        item(key = "h:torrents") {
                            SectionTitle("Active torrents", trailing = "${active.size} of ${data.torrents.size}")
                        }
                        if (active.isEmpty()) item(key = "e:torrents") { EmptyHint("No active torrents.") }
                        items(active.size, key = { "t:$it" }) { i -> TorrentRow(active[i]) }
                    }
                    data.sections.forEachIndexed { i, section -> detailSection(i, section) }
                }

            }
        }
    }
}

private fun LazyListScope.detailSection(index: Int, section: DetailSection) {
    val isTable = section.headers.isNotEmpty()
    item(key = "sec:$index") {
        SectionTitle(section.title)
        HomelabCard(Modifier.fillMaxWidth()) {
            Column {
                if (isTable) {
                    DetailTableHeader(section)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                // Long lists (e.g. hundreds of missing movies) are capped in one card for smooth scrolling.
                val rows = section.rows.take(MAX_ROWS)
                rows.forEachIndexed { i, row ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    DetailRowView(row, isTable)
                }
                if (section.rows.size > MAX_ROWS) {
                    Text(
                        "+ ${section.rows.size - MAX_ROWS} more",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

private const val MAX_ROWS = 200

@Composable
private fun Header(
    service: ServiceUi,
    baseUrl: String?,
    docker: DockerState?,
    health: HealthCheck?,
    onOpen: () -> Unit,
) {
    HomelabCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(HomelabTheme.colors.statBlock),
                contentAlignment = Alignment.Center,
            ) {
                ServiceIcon(service.icon, service.name, size = 56.dp, baseUrl = baseUrl)
            }
            Spacer(Modifier.height(10.dp))
            Text(service.name, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            service.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            Text(
                service.group,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (docker != null || health != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    docker?.let { StatusPill(it.label, it.level) }
                    health?.let { HealthPill(it) }
                }
            }
            if (service.href != null) {
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onOpen) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open")
                }
                Text(
                    service.href,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun DockerStatsCard(service: ServiceUi, docker: DockerState?, stats: DockerStats) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatRow(
            listOf(
                Stat("CPU", stats.cpuPercent?.let { String.format(Locale.US, "%.1f%%", it) } ?: "—"),
                Stat("Memory", stats.memoryBytes?.let { String.format(Locale.US, "%.0f MB", it / (1024 * 1024)) } ?: "—"),
                Stat("RX", stats.rxBytes?.let { Format.bytes(it) } ?: "—"),
                Stat("TX", stats.txBytes?.let { Format.bytes(it) } ?: "—"),
            ),
        )
        Text(
            listOfNotNull(
                service.container?.let { "container $it" },
                service.server?.let { "on $it" },
                docker?.let { "· ${it.status}" + (it.health?.let { h -> " ($h)" } ?: "") },
            ).joinToString(" "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
