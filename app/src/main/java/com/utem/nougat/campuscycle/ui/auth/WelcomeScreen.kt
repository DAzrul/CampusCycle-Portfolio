package com.utem.nougat.campuscycle.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utem.nougat.campuscycle.R
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import com.utem.nougat.campuscycle.ui.theme.CampusCycleTheme

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // 1. Ganti Color.White dengan warna tema background
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // --- BAHAGIAN ATAS: LOGO ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            // Logo Kotak Biru (Kekal biru & putih sebab brand color)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CampusBlue, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CampusCycle",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                // 2. Text utama ikut warna kontra background (Hitam/Putih)
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // --- BAHAGIAN TENGAH: GAMBAR & TEXT ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Your campus, your\nmarketplace for students",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                // 3. Text subtitle warna kelabu ikut tema
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // GAMBAR KAMPUS
            Image(
                painter = painterResource(id = R.drawable.campus_image),
                contentDescription = "Campus View",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        // --- BAHAGIAN BAWAH: BUTTON ---
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Button SIGN UP (Biru - Kekal)
            Button(
                onClick = onNavigateToRegister,
                colors = ButtonDefaults.buttonColors(containerColor = CampusBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Button LOG IN (Kelabu Cair - Dinamik)
            Button(
                onClick = onNavigateToLogin,
                colors = ButtonDefaults.buttonColors(
                    // 4. Button kelabu ni akan jadi gelap sikit kalau dark mode
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Log In", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    CampusCycleTheme {
        WelcomeScreen({}, {})
    }
}