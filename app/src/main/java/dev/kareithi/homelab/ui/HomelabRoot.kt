package dev.kareithi.homelab.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.kareithi.homelab.BuildConfig
import dev.kareithi.homelab.data.settings.Defaults
import dev.kareithi.homelab.ui.activity.ActivityScreen
import dev.kareithi.homelab.ui.bookmarks.BookmarksScreen
import dev.kareithi.homelab.ui.components.ConnectionIndicator
import dev.kareithi.homelab.ui.components.ShimmerHost
import dev.kareithi.homelab.ui.components.cardShadow
import dev.kareithi.homelab.ui.components.rememberNow
import dev.kareithi.homelab.ui.dashboard.DashboardScreen
import dev.kareithi.homelab.ui.dashboard.DashboardUiState
import dev.kareithi.homelab.ui.dashboard.DashboardViewModel
import dev.kareithi.homelab.ui.detail.ServiceDetailScreen
import dev.kareithi.homelab.ui.settings.SettingsScreen
import dev.kareithi.homelab.ui.settings.SettingsViewModel
import dev.kareithi.homelab.ui.theme.HomelabTheme
import dev.kareithi.homelab.ui.theme.Palette
import dev.kareithi.homelab.domain.Format
import kotlinx.coroutines.launch

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Dashboard),
    ACTIVITY("activity", "Activity", Icons.Filled.Bolt),
    BOOKMARKS("bookmarks", "Bookmarks", Icons.Filled.Bookmarks),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}

private const val DETAIL_ROUTE = "detail/{key}"
private fun detailRoute(key: String) = "detail/${Uri.encode(key)}"

@Composable
fun HomelabRoot() {
    // Activity-scoped: every screen shares one dashboard state and one refresh loop.
    val dashboardVm: DashboardViewModel = viewModel(factory = AppViewModels.Factory)
    val settingsVm: SettingsViewModel = viewModel(factory = AppViewModels.Factory)
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val onTopLevel = Tab.entries.any { it.route == currentRoute }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ShimmerHost {
    ModalNavigationDrawer(
        drawerState = drawerState,
        // Swipe-to-open only on top-level screens so it never fights the detail screen's back gesture.
        gesturesEnabled = onTopLevel || drawerState.isOpen,
        scrimColor = if (HomelabTheme.colors.isDark) Color.Black.copy(alpha = 0.7f) else Palette.Ink.copy(alpha = 0.45f),
        drawerContent = {
            val state by dashboardVm.state.collectAsStateWithLifecycle()
            val settings by dashboardVm.settings.collectAsStateWithLifecycle()
            FloatingDrawer(
                title = settings.title,
                state = state,
                currentRoute = currentRoute,
                onSelect = { tab ->
                    scope.launch { drawerState.close() }
                    nav.switchTab(tab.route)
                },
            )
        },
    ) {
        NavHost(
            navController = nav,
            startDestination = Tab.DASHBOARD.route,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Tab.DASHBOARD.route) {
                DashboardScreen(
                    vm = dashboardVm,
                    settingsVm = settingsVm,
                    onOpenService = { nav.navigate(detailRoute(it)) },
                    onOpenMenu = openMenu,
                )
            }
            composable(Tab.ACTIVITY.route) { ActivityScreen(dashboardVm, onOpenMenu = openMenu) }
            composable(Tab.BOOKMARKS.route) { BookmarksScreen(dashboardVm, onOpenMenu = openMenu) }
            composable(Tab.SETTINGS.route) { SettingsScreen(settingsVm, dashboardVm, onOpenMenu = openMenu) }
            composable(
                DETAIL_ROUTE,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
            ) { entry ->
                val key = entry.arguments?.getString("key").orEmpty()
                ServiceDetailScreen(serviceKey = key, vm = dashboardVm, onBack = { nav.popBackStack() })
            }
        }
    }
    }
}

/** Sidebar that floats with a margin, rounded corners and a border, like the top bar. */
@Composable
private fun FloatingDrawer(
    title: String,
    state: DashboardUiState,
    currentRoute: String?,
    onSelect: (Tab) -> Unit,
) {
    val c = HomelabTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val now = rememberNow()
    ModalDrawerSheet(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
            .width(296.dp)
            .fillMaxHeight()
            .cardShadow(shape, 20.dp)
            .border(1.dp, c.cardBorder, shape),
        drawerShape = shape,
        drawerContainerColor = c.card,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(c.accentFill, lerp(c.accentFill, c.card, 0.25f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Dns, contentDescription = null, tint = c.onAccentFill, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                Defaults.DESCRIPTION,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionIndicator(state.connection)
                state.lastUpdated?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Updated ${Format.relative(it, maxOf(now, it))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = c.cardBorder)
        Spacer(Modifier.height(12.dp))

        Tab.entries.forEach { tab ->
            val badge = when (tab) {
                Tab.ACTIVITY -> (state.nowPlaying.size + state.activeTorrents.count { !it.isComplete })
                    .takeIf { it > 0 }?.toString()
                Tab.DASHBOARD -> state.allServices.size.takeIf { it > 0 }?.toString()
                else -> null
            }
            NavigationDrawerItem(
                label = { Text(tab.label) },
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                badge = badge?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = c.accentFill,
                    selectedIconColor = c.onAccentFill,
                    selectedTextColor = c.onAccentFill,
                    selectedBadgeColor = c.onAccentFill,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = c.text,
                    unselectedBadgeColor = c.accentText,
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Homelab ${BuildConfig.VERSION_NAME}" + (state.baseUrl?.let { " · $it" } ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(20.dp),
        )
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
