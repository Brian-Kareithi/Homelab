package dev.kareithi.homelab.ui

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.kareithi.homelab.ui.activity.ActivityScreen
import dev.kareithi.homelab.ui.bookmarks.BookmarksScreen
import dev.kareithi.homelab.ui.dashboard.DashboardScreen
import dev.kareithi.homelab.ui.dashboard.DashboardViewModel
import dev.kareithi.homelab.ui.detail.ServiceDetailScreen
import dev.kareithi.homelab.ui.settings.SettingsScreen
import dev.kareithi.homelab.ui.settings.SettingsViewModel
import dev.kareithi.homelab.ui.theme.HomelabTheme

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
    val showBottomBar = Tab.entries.any { it.route == currentRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = HomelabTheme.colors.card) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = HomelabTheme.colors.accent,
                                selectedTextColor = HomelabTheme.colors.accent,
                                indicatorColor = HomelabTheme.colors.accent.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.DASHBOARD.route,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Tab.DASHBOARD.route) {
                DashboardScreen(
                    vm = dashboardVm,
                    settingsVm = settingsVm,
                    onOpenService = { nav.navigate(detailRoute(it)) },
                    onOpenSettings = { nav.switchTab(Tab.SETTINGS.route) },
                )
            }
            composable(Tab.ACTIVITY.route) { ActivityScreen(dashboardVm) }
            composable(Tab.BOOKMARKS.route) { BookmarksScreen(dashboardVm) }
            composable(Tab.SETTINGS.route) { SettingsScreen(settingsVm, dashboardVm) }
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

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
