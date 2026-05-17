package com.babymakisuk.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val TopBarIconSize: Dp = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BabyTopBar(
    title: @Composable () -> Unit,
    showSearch: Boolean = true,
    showAi: Boolean = true,
    showAdd: Boolean = false,
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAiClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    extraActions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    CenterAlignedTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "選單", // TODO: move to strings.xml
                    modifier = Modifier.size(TopBarIconSize)
                )
            }
        },
        actions = {
            extraActions()
            if (showSearch) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜尋", // TODO: move to strings.xml
                        modifier = Modifier.size(TopBarIconSize)
                    )
                }
            }
            if (showAi) {
                IconButton(onClick = onAiClick) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI", // TODO: move to strings.xml
                        tint = primaryColor,
                        modifier = Modifier.size(TopBarIconSize)
                    )
                }
            }
            if (showAdd) {
                IconButton(onClick = onAddClick) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "新增", // TODO: move to strings.xml
                        tint = primaryColor,
                        modifier = Modifier.size(TopBarIconSize)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            // Transparent so parent scaffold/gradient bleeds through the bar.
            containerColor = Color.Transparent
        )
    )
}
