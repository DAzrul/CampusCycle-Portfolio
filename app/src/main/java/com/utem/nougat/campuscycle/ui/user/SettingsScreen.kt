package com.utem.nougat.campuscycle.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToChangePassword: () -> Unit
) {
    // 1. Logic Warna Background (Sebiji macam gambar)
    // Light Mode: Kelabu Cair (F3F4F6)
    // Dark Mode: Hitam/Kelabu Gelap (Ikut Tema)
    val screenBackgroundColor = if (isDarkTheme) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)

    // 2. Logic Warna Top Bar
    val topBarColor = if (isDarkTheme) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color.Black

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor)
                .padding(padding)
                .padding(16.dp)
        ) {

            // --- Option 1: Change Password ---
            SettingsOptionItem(
                title = "Change Password",
                onClick = onNavigateToChangePassword,
                isDarkTheme = isDarkTheme // Pass info theme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Option 2: Theme (Toggle) ---
            // Kita bina manual kad ini sebab dia ada Text "Dark/Light" extra kat hujung
            Card(
                colors = CardDefaults.cardColors(
                    // Light Mode: Putih | Dark Mode: Kelabu Gelap
                    containerColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp) // Tetapkan tinggi supaya kemas dan center
                    .clickable { onThemeToggle() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize() // Penuhi kotak kad
                        .padding(horizontal = 16.dp), // Jarak kiri kanan
                    verticalAlignment = Alignment.CenterVertically // <--- PASTIKAN SENTIASA CENTER
                ) {
                    // Label "Theme"
                    Text(
                        "Theme",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                        color = textColor
                    )

                    // Status "Dark/Light"
                    Text(
                        text = if (isDarkTheme) "Dark" else "Light",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Arrow Icon
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// --- Komponen Item Settings (Reusable & Konsisten) ---
@Composable
fun SettingsOptionItem(
    title: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean // Terima parameter tema supaya warna konsisten
) {
    val cardColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val contentColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurface else Color.Black

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp) // Tinggi standard, sama macam Theme card
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically // <--- PASTIKAN SENTIASA CENTER
        ) {
            Text(
                title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f), // Tolak icon ke kanan
                color = contentColor
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}