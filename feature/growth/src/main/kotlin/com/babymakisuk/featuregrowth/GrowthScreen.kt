package com.babymakisuk.featuregrowth

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.babymakisuk.featuregrowth.ui.GrowthListScreen as GrowthScreenInternal

@Composable
fun GrowthScreen(
    navController: NavController
) = GrowthScreenInternal(navController = navController)
