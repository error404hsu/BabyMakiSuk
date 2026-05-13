package com.babymakisuk.navigation

import androidx.compose.ui.tooling.preview.Preview
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.babymakisuk.featureai.AiPortalScreen
import com.babymakisuk.featurehome.HomeScreen
import com.babymakisuk.featuregrowth.GrowthScreen
import com.babymakisuk.featuremedical.MedicalScreen
import com.babymakisuk.featurelibrary.LibraryScreen
import com.babymakisuk.featurelibrary.shelf.aiinsight.AiInsightShelfScreen
import com.babymakisuk.featurelibrary.shelf.memo.MemoShelfScreen
import com.babymakisuk.featurelibrary.shelf.weekly.WeeklyShelfScreen
import com.babymakisuk.featuresettings.ApiTestScreen
import com.babymakisuk.featuresettings.SettingsScreen
import com.babymakisuk.featureweeklyreport.WeeklyReportSearchScreen

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomNavItem("home", "首頁", Icons.Filled.Home)
    data object Growth : BottomNavItem("growth", "成長", Icons.Filled.ShowChart)
    data object Medical : BottomNavItem("medical", "就醫", Icons.Filled.Favorite)
    data object Library : BottomNavItem("library", "圖書", Icons.Filled.MenuBook)
    data object Settings : BottomNavItem("settings", "設定", Icons.Filled.Settings)
}

@Composable
fun BabyMakiSukNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = remember {
        listOf(
            BottomNavItem.Library,
            BottomNavItem.Growth,
            BottomNavItem.Home,
            BottomNavItem.Medical,
            BottomNavItem.Settings,
        )
    }

    // 子頁面路由不顯示 BottomBar
    val showBottomBar = currentDestination?.route?.let { route ->
        items.any { it.route == route }
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onNavigateToGrowth = {
                        navController.navigate(BottomNavItem.Growth.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToMedical = {
                        navController.navigate(BottomNavItem.Medical.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAi = { hint ->
                        navController.navigate("ai_portal?presetHint=$hint")
                    }
                )
            }
            composable(BottomNavItem.Growth.route) {
                GrowthScreen(
                    onNavigateToAi = { hint ->
                        navController.navigate("ai_portal?presetHint=$hint")
                    }
                )
            }
            composable(BottomNavItem.Medical.route) {
                MedicalScreen(
                    onNavigateToAi = { hint ->
                        navController.navigate("ai_portal?presetHint=$hint")
                    }
                )
            }
            composable(BottomNavItem.Library.route) {
                LibraryScreen(
                    navController = navController,
                    onNavigateToAi = { hint ->
                        navController.navigate("ai_portal?presetHint=$hint")
                    }
                )
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(
                    onNavigateToApiTest = { navController.navigate("settings/api_test") }
                )
            }
            composable("settings/api_test") {
                ApiTestScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // Sprint 3：週報 FTS 搜尋頁（子頁面，不顯示 BottomBar）
            composable(
                route = "weekly_report_search?childId={childId}",
                arguments = listOf(
                    navArgument("childId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getString("childId") ?: ""
                WeeklyReportSearchScreen(
                    navController = navController,
                    childId = childId
                )
            }
            composable(
                route = "ai_portal?presetHint={presetHint}",
                arguments = listOf(
                    navArgument("presetHint") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val presetHint = backStackEntry.arguments?.getString("presetHint")
                AiPortalScreen(
                    navController = navController,
                    presetHint = presetHint
                )
            }
            composable(
                route = "library/weekly?childId={childId}",
                arguments = listOf(
                    navArgument("childId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getString("childId") ?: ""
                WeeklyShelfScreen(
                    navController = navController,
                    childId = childId
                )
            }
            composable(
                route = "library/aiinsight?childId={childId}",
                arguments = listOf(
                    navArgument("childId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getString("childId") ?: ""
                AiInsightShelfScreen(
                    navController = navController,
                    childId = childId
                )
            }
            composable(
                route = "library/memo?childId={childId}",
                arguments = listOf(
                    navArgument("childId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val childId = backStackEntry.arguments?.getString("childId") ?: ""
                MemoShelfScreen(
                    navController = navController,
                    childId = childId
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BabyMakiSukNavHostPreview() {
    BabyMakiSukTheme {
        BabyMakiSukNavHost()
    }
}
