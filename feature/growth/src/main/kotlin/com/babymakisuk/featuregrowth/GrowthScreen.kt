package com.babymakisuk.featuregrowth

import androidx.compose.runtime.Composable
import com.babymakisuk.featuregrowth.ui.GrowthScreen as GrowthScreenInternal

// 蜈ｬ髢矩ｲ蜈･鮟橸ｼ御ｾ・NavHost 菴ｿ逕ｨ
@Composable
fun GrowthScreen(onNavigateToAi: (String?) -> Unit = {}) = GrowthScreenInternal(onNavigateToAi = onNavigateToAi)
