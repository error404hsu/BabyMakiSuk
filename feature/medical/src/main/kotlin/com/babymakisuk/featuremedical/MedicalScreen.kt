package com.babymakisuk.featuremedical

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender
import com.babymakisuk.coremodel.MedicalVisit

private val BoyBlue  = Color(0xFF4A90D9)
private val GirlPink = Color(0xFFE07BBD)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalScreen(viewModel: MedicalViewModel = hiltViewModel()) {
    val uiState  by viewModel.uiState.collectAsState()
    val showForm by viewModel.showForm.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("就醫紀錄") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openForm) {
                Icon(Icons.Filled.Add, contentDescription = "新增就診")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is MedicalUiState.Loading -> Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                is MedicalUiState.Error -> Box(Modifier.fillMaxSize()) {
                    Text("錯誤：${state.message}", Modifier.align(Alignment.Center))
                }
                is MedicalUiState.Success -> {
                    if (state.children.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("請先在設定中新增寶寶")
                        }
                    } else {
                        ChildFilterRow(
                            children = state.children,
                            selectedId = state.selectedChildId,
                            onSelect = viewModel::selectChild
                        )
                        HorizontalDivider()
                        if (state.visits.isEmpty()) {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("尚無就診紀錄，點擊 + 新增")
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.visits, key = { it.id }) { visit ->
                                    val child = state.children.firstOrNull { it.id == visit.childId }
                                    MedicalVisitCard(
                                        visit = visit,
                                        accentColor = if (child?.gender == Gender.MALE) BoyBlue else GirlPink,
                                        onDelete = { viewModel.deleteVisit(visit) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        val selectedChildId = (uiState as? MedicalUiState.Success)?.selectedChildId ?: 1L
        NewMedicalVisitDialog(
            childId = selectedChildId,
            onDismiss = viewModel::closeForm,
            onConfirm = viewModel::saveVisit
        )
    }
}

@Composable
private fun ChildFilterRow(
    children: List<ChildProfile>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        children.forEach { child ->
            val isSelected = child.id == selectedId
            val color = if (child.gender == Gender.MALE) BoyBlue else GirlPink
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(child.id) },
                label = {
                    Text(
                        text = "${if (child.gender == Gender.MALE) "\uD83D\uDC66" else "\uD83D\uDC67"} ${child.name}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.15f),
                    selectedLabelColor = color
                )
            )
        }
    }
}

@Composable
private fun MedicalVisitCard(
    visit: MedicalVisit,
    accentColor: Color,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        visit.hospital,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = buildString {
                            append(visit.date.toString())
                            if (visit.department.isNotBlank()) append("  ${visit.department}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "收合" else "展開",
                        tint = accentColor
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "刪除", tint = Color.LightGray)
                }
            }

            if (visit.diagnosis.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "診斷 ${visit.diagnosis}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242)
                )
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = accentColor.copy(alpha = 0.15f))
                Spacer(Modifier.height(10.dp))

                if (visit.diagnosisSummary.isNotBlank()) {
                    AiInfoCard(icon = "\uD83D\uDCCB", title = "AI 診斷摘要", content = visit.diagnosisSummary, color = accentColor)
                    Spacer(Modifier.height(8.dp))
                }
                if (visit.prescriptions.isNotBlank()) {
                    AiInfoCard(icon = "\uD83D\uDC8A", title = "處方", content = visit.prescriptions, color = accentColor)
                    Spacer(Modifier.height(8.dp))
                }
                if (visit.careInstructions.isNotBlank()) {
                    AiInfoCard(icon = "\uD83C\uDFE0", title = "居家照護須知", content = visit.careInstructions, color = accentColor)
                    Spacer(Modifier.height(8.dp))
                }
                if (visit.notes.isNotBlank()) {
                    AiInfoCard(icon = "\uD83D\uDCDD", title = "備註", content = visit.notes, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun AiInfoCard(icon: String, title: String, content: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "$icon $title",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(text = content, style = MaterialTheme.typography.bodySmall, color = Color(0xFF424242))
        }
    }
}
