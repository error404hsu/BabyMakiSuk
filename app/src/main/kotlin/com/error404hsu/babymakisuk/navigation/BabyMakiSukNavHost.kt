package com.error404hsu.babymakisuk.navigation

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.error404hsu.babymakisuk.featurehome.HomeScreen
import com.error404hsu.babymakisuk.featuregrowth.GrowthScreen
import com.error404hsu.babymakisuk.featuremedical.MedicalScreen
import com.error404hsu.babymakisuk.featurevaccine.VaccineScreen
import com.error404hsu.babymakisuk.featurelog.LogScreen
import com.error404hsu.babymakisuk.featuresettings.SettingsScreen

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    data object Home : BottomNavItem("home", "首頁", Icons.Filled.Home)
    data object Growth : BottomNavItem("growth", "成長", Icons.Filled.BarChart)
    data object Medical : BottomNavItem("medical", "醫療", Icons.Filled.LocalHospital)
    data object Vaccine : BottomNavItem("vaccine", "疫苗", Icons.Filled.Vaccines)
    data object Log : BottomNavItem("log", "日誌", Icons.Filled.Book)
    data object Settings : BottomNavItem("settings", "設定", Icons.Filled.Settings)
}

@Composable
fun BabyMakiSukNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = remember {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Growth,
            BottomNavItem.Medical,
            BottomNavItem.Vaccine,
            BottomNavItem.Log,
            BottomNavItem.Settings,
        )
    }

    Scaffold(
        bottomBar = {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.Growth.route) { GrowthScreen() }
            composable(BottomNavItem.Medical.route) { MedicalScreen() }
            composable(BottomNavItem.Vaccine.route) { VaccineScreen() }
            composable(BottomNavItem.Log.route) { LogScreen() }
            composable(BottomNavItem.Settings.route) { SettingsScreen() }
        }
    }
}
