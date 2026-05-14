package com.babymakisuk.navigation

import androidx.compose.ui.tooling.preview.Preview
import com.babymakisuk.ui.theme.BabyMakiSukTheme
import com.babymakisuk.ui.components.LocalDrawerState
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.babymakisuk.featurevaccine.VaccineScreen
import com.babymakisuk.featurelibrary.LibraryScreen
import com.babymakisuk.featurelibrary.shelf.aiinsight.AiInsightShelfScreen
import com.babymakisuk.featurelibrary.shelf.memo.MemoEditScreen
import com.babymakisuk.featurelibrary.shelf.memo.MemoShelfScreen
import com.babymakisuk.featurelibrary.shelf.weekly.WeeklyShelfScreen
import com.babymakisuk.featuresettings.ApiTestScreen
import com.babymakisuk.featuresettings.SettingsScreen
import com.babymakisuk.featureweeklyreport.WeeklyReportScreen
import com.babymakisuk.featureweeklyreport.WeeklyReportSearchScreen
import kotlinx.coroutines.launch

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Vaccine : BottomNavItem("vaccine", "疫苗 就醫", Icons.Filled.HealthAndSafety)
    data object Medical : BottomNavItem("medical", "就醫", Icons.Filled.Favorite)
    data object Home : BottomNavItem("home", "首頁", Icons.Filled.Home)
    data object Growth : BottomNavItem("growth", "成長", Icons.AutoMirrored.Filled.ShowChart)
    data object Settings : BottomNavItem("settings", "設定", Icons.Filled.Settings)
}

@Composable
fun BabyMakiSukNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = remember {
        listOf(
            BottomNavItem.Vaccine,
            BottomNavItem.Medical,
            BottomNavItem.Home,
            BottomNavItem.Growth,
            BottomNavItem.Settings,
        )
    }

    val showBottomBar = currentDestination?.route?.let { route ->
        items.any { it.route == route }
    } ?: true

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalDrawerState provides drawerState) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Text(
                        text = "書庫",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(24.dp, 16.dp)
                    )
                    LibraryScreen(
                        navController = navController,
                        onNavigateToAi = { hint ->
                            scope.launch { drawerState.close() }
                            navController.navigate("ai_portal?presetHint=$hint")
                        }
                    )
                }
            }
        ) {
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
                            },
                            onNavigateToMemoEdit = { childId ->
                                navController.navigate("library/memo/edit?memoId=-1&childId=$childId")
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
                    composable(BottomNavItem.Vaccine.route) {
                        VaccineScreen(
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
                        route = "weekly_report?childId={childId}",
                        arguments = listOf(
                            navArgument("childId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = ""
                            }
                        )
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getString("childId") ?: ""
                        WeeklyReportScreen(
                            childId = childId,
                            onBack = { navController.popBackStack() }
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
                    composable(
                        route = "library/memo/edit?memoId={memoId}&childId={childId}",
                        arguments = listOf(
                            navArgument("memoId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            },
                            navArgument("childId") {
                                type = NavType.LongType
                                defaultValue = -1L
                            }
                        )
                    ) { backStackEntry ->
                        val memoId = backStackEntry.arguments?.getLong("memoId") ?: -1L
                        val childId = backStackEntry.arguments?.getLong("childId") ?: -1L
                        MemoEditScreen(
                            navController = navController,
                            memoId = memoId,
                            childId = childId
                        )
                    }
                }
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
