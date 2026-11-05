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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.kareithi.homelab.data.settings.Defaults
import dev.kareithi.homelab.domain.ServiceGroupUi
import dev.kareithi.homelab.ui.components.AccentBar
import dev.kareithi.homelab.ui.components.AutoRefreshEffect
import dev.kareithi.homelab.ui.components.CountChip
import dev.kareithi.homelab.ui.components.GroupHeaderSkeleton
import dev.kareithi.homelab.ui.components.ResourcesSkeleton
import dev.kareithi.homelab.ui.components.ServiceCardSkeleton
import dev.kareithi.homelab.ui.components.ConnectionIndicator
import dev.kareithi.homelab.ui.components.HomelabScaffold
import dev.kareithi.homelab.ui.components.NavAction
import dev.kareithi.homelab.ui.components.OfflineBanner
import dev.kareithi.homelab.ui.components.ResourcesStrip
import dev.kareithi.homelab.ui.components.rememberNow
import dev.kareithi.homelab.ui.components.rememberUrlOpener
import dev.kareithi.homelab.ui.settings.SettingsViewModel
import dev.kareithi.homelab.ui.theme.HomelabTheme
import java.util.Locale

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
                val res = state.resources
                when {
                    res != null && !res.isEmpty -> fullWidth("resources") { ResourcesStrip(res) }
                    res == null && !state.initialAttemptDone -> fullWidth("resources:skeleton") { ResourcesSkeleton() }
                }

                if (!state.hasData) {
                    dashboardSkeleton(columns)
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

/** Skeleton that mirrors the real layout: two groups of service cards. */
private fun LazyGridScope.dashboardSkeleton(columns: Int) {
    listOf(4, 2).forEachIndexed { g, cards ->
        item(key = "skeleton:group:$g", span = { GridItemSpan(maxLineSpan) }) { GroupHeaderSkeleton() }
        items(count = cards * columns, key = { "skeleton:$g:$it" }) { i ->
            ServiceCardSkeleton(stats = if (i % 3 == 2) 0 else 3 + i % 2)
        }
    }
}

@Composable
private fun GroupHeader(group: ServiceGroupUi, collapsed: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(if (collapsed) -90f else 0f, label = "chevron")
    val c = HomelabTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccentBar()
        Spacer(Modifier.width(10.dp))
        Text(
            group.name.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        CountChip("${group.services.size}")
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = if (collapsed) "Expand" else "Collapse",
            tint = c.accent,
            modifier = Modifier.rotate(rotation),
        )
    }
}
