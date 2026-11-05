package dev.kareithi.homelab.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.domain.Format
import dev.kareithi.homelab.ui.components.AutoRefreshEffect
import dev.kareithi.homelab.ui.components.ConnectionIndicator
import dev.kareithi.homelab.ui.components.EmptyHint
import dev.kareithi.homelab.ui.components.HomelabScaffold
import dev.kareithi.homelab.ui.components.ListRowSkeleton
import dev.kareithi.homelab.ui.components.NavAction
import dev.kareithi.homelab.ui.components.NowPlayingRow
import dev.kareithi.homelab.ui.components.OfflineBanner
import dev.kareithi.homelab.ui.components.QueueRow
import dev.kareithi.homelab.ui.components.SectionTitle
import dev.kareithi.homelab.ui.components.TorrentRow
import dev.kareithi.homelab.ui.components.rememberNow
import dev.kareithi.homelab.ui.dashboard.DashboardViewModel
import dev.kareithi.homelab.ui.dashboard.rememberColumns
import java.time.Instant

/** "What's happening right now": playback, *arr queue and active torrents in one place. */
@Composable
fun ActivityScreen(vm: DashboardViewModel, onOpenMenu: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val nowMillis = rememberNow()
    val now = Instant.ofEpochMilli(nowMillis)

    AutoRefreshEffect(settings.refreshSeconds) { vm.autoRefresh() }

    val sessions = state.nowPlaying
    val queue = state.queue
    val torrents = state.activeTorrents
    val totalDown = torrents.sumOf { it.downloadSpeed }
    val totalUp = torrents.sumOf { it.uploadSpeed }
    // First load (nothing cached yet): show skeletons shaped like the real rows.
    val loading = (!state.hasData && !state.initialAttemptDone) ||
        state.widgets.values.any { it.loading && it.data == null }

    HomelabScaffold(
        title = "Activity",
        subtitle = "What's happening right now",
        navAction = NavAction.MENU,
        onNavAction = onOpenMenu,
        actions = { ConnectionIndicator(state.connection, Modifier.padding(end = 4.dp)) },
        isRefreshing = state.isUserRefreshing,
        onRefresh = { vm.refresh(userInitiated = true) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(rememberColumns()),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.isOffline) {
                full("offline") { OfflineBanner(state.lastUpdated, state.offlineMessage, nowMillis) }
            }

            full("h:playing") { SectionTitle("Now playing", trailing = sessions.size.toString()) }
            if (sessions.isEmpty() && loading) {
                skeletons("s:playing", 1, progress = true)
            } else if (sessions.isEmpty()) {
                full("e:playing") { EmptyHint("Nothing is playing right now.") }
            } else {
                items(sessions.size, key = { "np:$it" }) { i -> NowPlayingRow(sessions[i]) }
            }

            full("h:queue") {
                val total = state.queueTotal
                SectionTitle(
                    "Download queue",
                    trailing = if (total > queue.size) "${queue.size} of $total" else queue.size.toString(),
                )
            }
            if (queue.isEmpty() && loading) {
                skeletons("s:queue", 3, progress = true)
            } else if (queue.isEmpty()) {
                full("e:queue") { EmptyHint("The Sonarr queue is empty.") }
            } else {
                items(queue.size, key = { "q:$it" }) { i -> QueueRow(queue[i], now) }
            }

            full("h:torrents") {
                SectionTitle(
                    "Torrents",
                    trailing = if (torrents.isEmpty()) "0"
                    else "↓ ${Format.speed(totalDown)}  ↑ ${Format.speed(totalUp)}",
                )
            }
            if (torrents.isEmpty() && loading) {
                skeletons("s:torrents", 3, progress = true)
            } else if (torrents.isEmpty()) {
                full("e:torrents") { EmptyHint("No active torrents.") }
            } else {
                items(torrents.size, key = { "t:$it" }) { i -> TorrentRow(torrents[i]) }
            }
        }
    }
}

private fun LazyGridScope.skeletons(prefix: String, count: Int, progress: Boolean) {
    items(count, key = { "$prefix:$it" }) { ListRowSkeleton(leading = false, progress = progress) }
}

private fun LazyGridScope.full(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}
