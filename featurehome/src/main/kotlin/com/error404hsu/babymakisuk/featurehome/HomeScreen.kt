package com.error404hsu.babymakisuk.featurehome

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.error404hsu.babymakisuk.coremodel.ChildProfile

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val children by viewModel.children.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("BabyMakiSuk") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onAddChildClick) {
                Icon(Icons.Filled.Add, contentDescription = "新增幼兒")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(children) { child ->
                ChildCard(child)
            }
        }
    }
}

@Composable
private fun ChildCard(child: ChildProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = child.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "生日：${child.birthday}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
