package com.utem.nougat.campuscycle.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@Composable
fun ProfileAdminPage(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit
) {
    // --- STATE ---
    var adminName by remember { mutableStateOf("-") }
    var adminEmail by remember { mutableStateOf("-") }
    var adminId by remember { mutableStateOf("-") }
    var adminPhone by remember { mutableStateOf("-") }
    var photoUrl by remember { mutableStateOf("") }

    val context = LocalContext.current

    // --- LOGIC DARK MODE ---
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
    val subTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    // --- TARIK DATA LIVE DARI FIREBASE ---
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            adminEmail = user.email ?: "-"
            FirebaseFirestore.getInstance().collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        adminName = snapshot.getString("fullName") ?: "-"
                        adminId = snapshot.getString("studentId") ?: "-"
                        adminPhone = snapshot.getString("phoneNumber") ?: "-"
                        photoUrl = snapshot.getString("photoUrl") ?: ""
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            Icon(
                Icons.Outlined.Settings,
                "Settings",
                tint = textColor,
                modifier = Modifier.clickable { onSettingsClick() }
            )
        }

        // --- CONTENT ---
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. KAD PROFIL UTAMA
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- GAMBAR PROFIL ---
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.DarkGray else Color(0xFFE0E0E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (adminName.length > 1) adminName.take(1).uppercase() else "A",
                                fontSize = 40.sp,
                                color = if (isDark) Color.White else Color.DarkGray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // INFO TEXT
                    Text(
                        text = adminName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = adminId, color = subTextColor, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text(text = adminPhone, color = subTextColor, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Text(text = adminEmail, color = subTextColor, fontSize = 14.sp, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(24.dp))

                    // STATS (Contoh Statik)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(count = "12", label = "Listings", textColor = textColor, subTextColor = subTextColor)
                        StatItem(count = "5", label = "Favorites", textColor = textColor, subTextColor = subTextColor)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // EDIT BUTTON
                    OutlinedButton(
                        onClick = onEditProfile,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.Gray else Color.LightGray),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. MENU OPTIONS
            MenuOptionItem(Icons.Outlined.List, "My Listings", cardColor, textColor, onClick = {})
            Spacer(modifier = Modifier.height(12.dp))

            MenuOptionItem(Icons.Outlined.FavoriteBorder, "Favorites", cardColor, textColor, onClick = {})
            Spacer(modifier = Modifier.height(12.dp))

            MenuOptionItem(Icons.Outlined.Settings, "Settings", cardColor, textColor, onClick = { onSettingsClick() })
            Spacer(modifier = Modifier.height(12.dp))

            // 3. LOGOUT (LOGIC BARU)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().clickable {

                    // A. SETUP PREFERENCES
                    val masterKey = MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    val sharedPreferences = EncryptedSharedPreferences.create(
                        context,
                        "SecureCampusPrefs", // Nama fail MESTI SAMA dengan LoginScreen
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )

                    // B. PADAM DATA REMEMBER ME
                    sharedPreferences.edit().clear().apply()

                    // C. FIREBASE SIGNOUT
                    FirebaseAuth.getInstance().signOut()

                    // D. NAVIGATE KELUAR
                    onLogout()
                }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun StatItem(count: String, label: String, textColor: Color, subTextColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
        Text(label, color = subTextColor, fontSize = 12.sp)
    }
}

@Composable
fun MenuOptionItem(icon: ImageVector, title: String, cardColor: Color, textColor: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = textColor, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AdminProfilePage(
    onLogout: () -> Unit, // Ini callback navigation dkt MainActivity kau
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(100.dp), tint = CampusBlue)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Admin Account", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) { Text("Edit Profile") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSettingsClick, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
        Spacer(modifier = Modifier.height(24.dp))

        // --- LOGIC LOGOUT YANG FUCKING CLEAN ---
        TextButton(onClick = {
            if (currentUser != null) {
                // 1. Clear token dkt Firestore dulu!
                db.collection("users").document(currentUser.uid)
                    .update("fcmToken", null)
                    .addOnCompleteListener {
                        // 2. Baru sign out
                        FirebaseAuth.getInstance().signOut()
                        onLogout() // Navigate balik ke Login Screen
                        Toast.makeText(context, "Admin Logged Out Safely!", Toast.LENGTH_SHORT).show()
                    }
            }
        }) {
            Text("Log Out", color = Color.Red, fontWeight = FontWeight.Bold)
        }
    }
}