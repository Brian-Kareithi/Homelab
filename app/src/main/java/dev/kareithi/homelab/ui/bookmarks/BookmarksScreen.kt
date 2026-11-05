package dev.kareithi.homelab.ui.bookmarks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.domain.BookmarkUi
import dev.kareithi.homelab.ui.components.CardShape
import dev.kareithi.homelab.ui.components.EmptyHint
import dev.kareithi.homelab.ui.components.HomelabCard
import dev.kareithi.homelab.ui.components.LetterTile
import dev.kareithi.homelab.ui.components.SectionTitle
import dev.kareithi.homelab.ui.components.ServiceIcon
import dev.kareithi.homelab.ui.components.rememberUrlOpener
import dev.kareithi.homelab.ui.dashboard.DashboardViewModel
import dev.kareithi.homelab.ui.dashboard.rememberColumns

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(vm: DashboardViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val openUrl = rememberUrlOpener()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isUserRefreshing,
            onRefresh = { vm.refresh(userInitiated = true) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(rememberColumns()),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.bookmarks.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Spacer(Modifier.padding(4.dp))
                            EmptyHint(if (state.isRefreshing) "Loading bookmarks…" else "No bookmarks configured in Homepage.")
                        }
                    }
                }
                state.bookmarks.forEachIndexed { gi, group ->
                    item(key = "g:$gi", span = { GridItemSpan(maxLineSpan) }) {
                        SectionTitle(group.name, trailing = group.bookmarks.size.toString())
                    }
                    items(group.bookmarks.size, key = { "b:$gi:$it" }) { bi ->
                        val b = group.bookmarks[bi]
                        BookmarkRow(b, baseUrl = state.baseUrl, onClick = { openUrl(b.href) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkRow(b: BookmarkUi, baseUrl: String?, onClick: () -> Unit) {
    HomelabCard(
        Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (b.icon.isNullOrBlank()) LetterTile(b.abbr?.takeIf { it.isNotBlank() } ?: b.name, 36.dp)
            else ServiceIcon(b.icon, b.name, size = 36.dp, baseUrl = baseUrl)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    b.description ?: (b.href.toUri().host ?: b.href),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
