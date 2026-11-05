package dev.kareithi.homelab.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.data.settings.Defaults
import dev.kareithi.homelab.domain.ServiceGroupUi
import dev.kareithi.homelab.ui.components.AutoRefreshEffect
import dev.kareithi.homelab.ui.components.ConnectionIndicator
import dev.kareithi.homelab.ui.components.HomelabCard
import dev.kareithi.homelab.ui.components.HomelabScaffold
import dev.kareithi.homelab.ui.components.NavAction
import dev.kareithi.homelab.ui.components.OfflineBanner
import dev.kareithi.homelab.ui.components.ResourcesStrip
import dev.kareithi.homelab.ui.components.ShimmerStatRow
import dev.kareithi.homelab.ui.components.rememberNow
import dev.kareithi.homelab.ui.components.rememberUrlOpener
import dev.kareithi.homelab.ui.components.shimmer
import dev.kareithi.homelab.ui.settings.SettingsViewModel

/** 1 column on phones in portrait, 2 in landscape / on tablets (width ≥ 600dp). */
@Composable
fun rememberColumns(): Int = if (LocalConfiguration.current.screenWidthDp >= 600) 2 else 1

@Composable
fun DashboardScreen(
    vm: DashboardViewModel,
    settingsVm: SettingsViewModel,
    onOpenService: (String) -> Unit,
    onOpenMenu: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val openUrl = rememberUrlOpener()
    val now = rememberNow()

    AutoRefreshEffect(settings.refreshSeconds) { vm.autoRefresh() }

    HomelabScaffold(
        title = settings.title,
        subtitle = Defaults.DESCRIPTION,
        navAction = NavAction.MENU,
        onNavAction = onOpenMenu,
        actions = { ConnectionIndicator(state.connection) },
        isRefreshing = state.isUserRefreshing,
        onRefresh = if (state.needsSetup) null else ({ vm.refresh(userInitiated = true) }),
    ) { padding ->
        if (state.needsSetup) {
            SetupScreen(
                dashboardState = state,
                settingsVm = settingsVm,
                onRetry = { vm.refresh(userInitiated = true, forceResolve = true) },
                modifier = Modifier.padding(padding),
            )
            return@HomelabScaffold
        }

        run {
            val columns = rememberColumns()
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.isOffline && state.hasData) {
                    fullWidth("offline") {
                        OfflineBanner(state.lastUpdated, state.offlineMessage, now)
                    }
                }
                state.resources?.takeIf { !it.isEmpty }?.let { res ->
                    fullWidth("resources") { ResourcesStrip(res) }
                }

                if (!state.hasData) {
                    loadingPlaceholders(columns)
                }

                state.groups.forEachIndexed { gi, group ->
                    val collapsed = group.name in settings.collapsedGroups
                    fullWidth("group:$gi") {
                        GroupHeader(group, collapsed) { vm.toggleGroup(group.name) }
                    }
                    if (!collapsed) {
                        group.services.forEachIndexed { si, svc ->
                            item(key = "svc:$gi:$si") {
                                ServiceCard(
                                    service = svc,
                                    widget = state.widgets[svc.key],
                                    docker = state.docker[svc.key],
                                    health = state.health[svc.key],
                                    baseUrl = state.baseUrl,
                                    onClick = { onOpenService(svc.key) },
                                    onLongClick = { svc.href?.let(openUrl) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.fullWidth(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

private fun LazyGridScope.loadingPlaceholders(columns: Int) {
    items(count = 4 * columns, key = { "placeholder:$it" }) {
        HomelabCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(40.dp).height(40.dp).clip(MaterialTheme.shapes.small).shimmer())
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Box(Modifier.width(120.dp).height(14.dp).clip(MaterialTheme.shapes.small).shimmer())
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.width(80.dp).height(10.dp).clip(MaterialTheme.shapes.small).shimmer())
                    }
                }
                Spacer(Modifier.height(10.dp))
                ShimmerStatRow(3)
            }
        }
    }
}

@Composable
private fun GroupHeader(group: ServiceGroupUi, collapsed: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "chevron")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onToggle)
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            group.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${group.services.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = if (collapsed) "Expand" else "Collapse",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation),
        )
    }
}
