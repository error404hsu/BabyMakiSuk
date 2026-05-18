package com.babymakisuk.featuregrowth.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.babymakisuk.coremodel.ChildProfile
import com.babymakisuk.coremodel.Gender

@Composable
fun ChildSelectorRow(
    children: List<ChildProfile>,
    selectedChildId: Long,
    onSelectChild: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(children) { child ->
            val isSelected = child.id == selectedChildId
            val childColor = if (child.gender == Gender.MALE) BoyBlue else GirlPink

            Surface(
                onClick = { onSelectChild(child.id) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) childColor else childColor.copy(alpha = 0.1f),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) childColor else childColor.copy(alpha = 0.3f)
                ),
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color.White.copy(alpha = 0.2f) else childColor.copy(alpha = 0.1f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (child.gender == Gender.MALE) "\uD83D\uDC66" else "\uD83D\uDC67", fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else childColor
                    )
                }
            }
        }
    }
}
