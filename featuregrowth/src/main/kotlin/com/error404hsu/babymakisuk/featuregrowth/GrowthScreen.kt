package com.error404hsu.babymakisuk.featuregrowth

import androidx.compose.runtime.Composable
import com.error404hsu.babymakisuk.featuregrowth.ui.GrowthScreen as GrowthScreenInternal

// 公開進入點，供 NavHost 使用
@Composable
fun GrowthScreen() = GrowthScreenInternal()
