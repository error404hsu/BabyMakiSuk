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
import androidx.compose.ui.tooling.preview.Preview
import com.error404hsu.babymakisuk.coremodel.ChildProfile
import com.error404hsu.babymakisuk.coremodel.Gender
import com.error404hsu.babymakisuk.ui.theme.BabyMakiSukTheme
import java.time.LocalDate

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val children by viewModel.children.collectAsState(initial = emptyList())
    HomeScreen(
        children = children,
        onAddChildClick = viewModel::onAddChildClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    children: List<ChildProfile>,
    onAddChildClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("BabyMakiSuk") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddChildClick) {
                Icon(Icons.Filled.Add, contentDescription = "新增幼兒")
            }
        },
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(children) { child ->
                ChildCard(child)
            }
        }
    }
}

@Composable
fun ChildCard(child: ChildProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = child.name, style = MaterialTheme.typography.titleLarge)
            Text(text = "生日: ${child.birthday}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BabyMakiSukTheme {
        HomeScreen(
            children = listOf(
                ChildProfile(
                    id = 1,
                    name = "小明",
                    gender = Gender.MALE,
                    birthday = LocalDate.now().minusYears(1)
                ),
                ChildProfile(
                    id = 2,
                    name = "小華",
                    gender = Gender.FEMALE,
                    birthday = LocalDate.now().minusMonths(6)
                )
            ),
            onAddChildClick = {},
        )
    }
}
