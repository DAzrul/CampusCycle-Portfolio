package com.utem.nougat.campuscycle.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@Composable
fun AdminAnalyticsPage() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF8F9FB)
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black

    val db = FirebaseFirestore.getInstance()
    var totalSold by remember { mutableLongStateOf(450L) } // Hardcode contoh mockup
    var totalSavings by remember { mutableDoubleStateOf(15400.0) } // Hardcode contoh mockup

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("CampusCycle", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = textColor)
        Spacer(modifier = Modifier.height(24.dp))

        // STATS CARDS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            UniqueStatCard("Total Items Sold", "$totalSold", Icons.Default.ArrowUpward, Modifier.weight(1f), cardColor, textColor)
            UniqueStatCard("Est. Student Savings", "RM ${String.format("%,.0f", totalSavings)}", Icons.Default.MonetizationOn, Modifier.weight(1f), cardColor, textColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // MARKETPLACE STATUS (Donut Chart)
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardColor), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Marketplace Status", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start), color = textColor)
                Spacer(modifier = Modifier.height(30.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.size(160.dp)) {
                        // FIX: Added 'useCenter = false' and corrected parameters
                        drawArc(Color(0xFF03A9F4), -90f, 216f, false, style = Stroke(25.dp.toPx(), cap = StrokeCap.Round))
                        drawArc(Color(0xFF4CAF50), 126f, 144f, false, style = Stroke(25.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Text("40% Sold", fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TOP CATEGORIES DEMAND
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardColor), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Top Categories Demand", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                Spacer(modifier = Modifier.height(16.dp))
                DemandBar("Textbooks", 0.8f, Color(0xFF03A9F4))
                DemandBar("Electronics", 0.65f, Color(0xFF00E5FF))
                DemandBar("Dorm Needs", 0.5f, Color(0xFF7C4DFF))
                DemandBar("Clothing", 0.3f, Color(0xFFFFB300))
            }
        }
    }
}

@Composable
private fun DemandBar(label: String, progress: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp)
            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = color, trackColor = Color.LightGray.copy(alpha = 0.2f))
    }
}

@Composable
private fun UniqueStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier, cardColor: Color, textColor: Color) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = cardColor), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 11.sp, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                Spacer(modifier = Modifier.weight(1f))
                Icon(icon, null, tint = CampusBlue, modifier = Modifier.size(20.dp))
            }
        }
    }
}