package com.notivault.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import com.notivault.ui.screen.*

object Routes {
    const val ONBOARDING = "onboarding"
    const val MAIN = "main"
    const val APP_DETAIL = "app_detail/{packageName}/{appLabel}"
    const val NOTIFICATION_DETAIL = "notification_detail/{notificationId}"
    const val RULES = "rules"
    const val APP_CONFIG = "app_config/{packageName}/{appLabel}"
    const val EXPORT = "export"
    const val SERVICE_HEALTH = "service_health"
    const val STORAGE = "storage"
    const val IGNORED_APPS = "ignored_apps"

    fun appDetail(packageName: String, appLabel: String) =
        "app_detail/$packageName/$appLabel"

    fun notificationDetail(notificationId: Long) =
        "notification_detail/$notificationId"

    fun appConfig(packageName: String, appLabel: String) =
        "app_config/$packageName/$appLabel"
}

// Bottom Nav Routes
object MainRoutes {
    const val HOME = "home"
    const val ACTIVITY = "activity"
    const val SUGGESTIONS = "suggestions"
    const val SETTINGS = "settings"
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(MainRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(MainRoutes.ACTIVITY, "Activity", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    BottomNavItem(MainRoutes.SUGGESTIONS, "Suggestions", Icons.Filled.Lightbulb, Icons.Outlined.Lightbulb),
    BottomNavItem(MainRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun NotiVaultNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAIN) {
            MainScreen(rootNavController = navController)
        }

        composable(
            route = Routes.APP_DETAIL,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("appLabel") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            val appLabel = backStackEntry.arguments?.getString("appLabel") ?: return@composable
            AppDetailScreen(
                packageName = packageName,
                appLabel = appLabel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.NOTIFICATION_DETAIL,
            arguments = listOf(
                navArgument("notificationId") { type = NavType.LongType }
            )
        ) {
            NotificationDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RULES) {
            RulesScreen(
                onBack = { navController.popBackStack() },
                onAppConfig = { packageName, appLabel ->
                    navController.navigate(Routes.appConfig(packageName, appLabel))
                }
            )
        }

        composable(
            route = Routes.APP_CONFIG,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("appLabel") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            val appLabel = backStackEntry.arguments?.getString("appLabel") ?: return@composable
            AppConfigScreen(
                packageName = packageName,
                appLabel = appLabel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EXPORT) {
            ExportScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SERVICE_HEALTH) {
            ServiceHealthScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STORAGE) {
            StorageScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.IGNORED_APPS) {
            AppPriorityScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun MainScreen(rootNavController: NavHostController) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            Column {
                HorizontalDivider(color = com.notivault.ui.theme.BorderColor, thickness = 1.dp)
                NavigationBar(
                    modifier = Modifier.height(76.dp),
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = com.notivault.ui.theme.Ink500,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            onClick = {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.notivault.ui.theme.Primary,
                                selectedTextColor = com.notivault.ui.theme.Primary,
                                indicatorColor = com.notivault.ui.theme.PrimarySoft,
                                unselectedIconColor = com.notivault.ui.theme.Ink500,
                                unselectedTextColor = com.notivault.ui.theme.Ink500
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = bottomNavController,
            startDestination = MainRoutes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(MainRoutes.HOME) {
                HomeScreen(
                    onAppClick = { packageName, appLabel ->
                        rootNavController.navigate(Routes.appDetail(packageName, appLabel))
                    }
                )
            }
            composable(MainRoutes.ACTIVITY) {
                ActivityScreen(
                    onNotificationClick = { notificationId ->
                        rootNavController.navigate(Routes.notificationDetail(notificationId))
                    }
                )
            }
            composable(MainRoutes.SUGGESTIONS) {
                SuggestionsScreen()
            }
            composable(MainRoutes.SETTINGS) {
                SettingsScreen(
                    onRulesClick = { rootNavController.navigate(Routes.RULES) },
                    onExportClick = { rootNavController.navigate(Routes.EXPORT) },
                    onServiceHealthClick = { rootNavController.navigate(Routes.SERVICE_HEALTH) },
                    onStorageClick = { rootNavController.navigate(Routes.STORAGE) },
                    onIgnoredAppsClick = { rootNavController.navigate(Routes.IGNORED_APPS) }
                )
            }
        }
    }
}
