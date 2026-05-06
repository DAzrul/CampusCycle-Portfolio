package com.utem.nougat.campuscycle.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.auth.AuthViewModel
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToMyListings: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
) {
    // --- STATE VARIABLES ---
    var currentUser by remember { mutableStateOf<FirebaseUser?>(null) }
    var studentName by remember { mutableStateOf("Loading...") }
    var studentEmail by remember { mutableStateOf("-") }
    var studentId by remember { mutableStateOf("-") }
    var studentPhone by remember { mutableStateOf("-") }
    var photoUrl by remember { mutableStateOf("") }
    var qrCodeUrl by remember { mutableStateOf("") }
    var businessLink by remember { mutableStateOf("") }
    var listingCount by remember { mutableStateOf("0") }
    var favoriteCount by remember { mutableStateOf("0") }
    var showContactDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF5F6F8)
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
    val subTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    // 1. DENGAR AUTH STATE (Supaya currentUser sentiasa update)
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { authState ->
            currentUser = authState.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // 2. TARIK DATA (Jalan setiap kali currentUser berubah)
    DisposableEffect(currentUser) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val db = FirebaseFirestore.getInstance()
            studentEmail = user.email ?: "-"

            // Listener: Profile
            val profileReg = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        studentName = "Error"
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        studentName = snapshot.getString("fullName") ?: "No Name"
                        studentId = snapshot.getString("studentId") ?: "-"
                        studentPhone = snapshot.getString("phoneNumber") ?: "-"
                        photoUrl = snapshot.getString("photoUrl") ?: ""
                        qrCodeUrl = snapshot.getString("qrCodeUrl") ?: ""
                        businessLink = snapshot.getString("businessLink") ?: ""
                    }
                }

            // Listener: Count
            val listReg = db.collection("products").whereEqualTo("sellerAuthId", user.uid)
                .addSnapshotListener { s, _ -> listingCount = (s?.size() ?: 0).toString() }
            val favReg = db.collection("favorites").whereEqualTo("userId", user.uid)
                .addSnapshotListener { s, _ -> favoriteCount = (s?.size() ?: 0).toString() }

            onDispose {
                profileReg.remove()
                listReg.remove()
                favReg.remove()
            }
        } else {
            // Kalau user logout, reset semua jadi default
            studentName = "Loading..."
            studentId = "-"
            onDispose { }
        }
    }

    if (showContactDialog) {
        Dialog(onDismissRequest = { showContactDialog = false }) {
            Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("My Contact Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (qrCodeUrl.isNotEmpty()) AsyncImage(model = qrCodeUrl, contentDescription = null, modifier = Modifier.size(200.dp))
                    else Text("No QR Code", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (businessLink.isNotEmpty()) SelectionContainer { Text(businessLink, color = CampusBlue) }
                    else Text("No Link", color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { showContactDialog = false }) { Text("Close") }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        Row(modifier = Modifier.fillMaxWidth().background(if (isDark) MaterialTheme.colorScheme.surface else Color.White).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            Icon(Icons.Outlined.Settings, "Settings", tint = textColor, modifier = Modifier.clickable { onSettingsClick() })
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // ... (KOD UI KEKAL SAMA) ...
            Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.Gray), contentAlignment = Alignment.Center) {
                        if (photoUrl.isNotEmpty()) AsyncImage(model = photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        else Text(studentName.take(1).uppercase(), fontSize = 40.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(studentName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Text(studentId, fontSize = 14.sp, color = subTextColor)
                    Text(studentEmail, fontSize = 14.sp, color = subTextColor)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        UserStatItem(listingCount, "Listings", textColor, subTextColor)
                        UserStatItem(favoriteCount, "Favorites", textColor, subTextColor)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) { Text("Edit Profile") }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            UserMenuOptionItem(Icons.Outlined.List, "My Listings", cardColor, textColor) { onNavigateToMyListings() }
            Spacer(modifier = Modifier.height(12.dp))
            UserMenuOptionItem(Icons.Outlined.QrCode, "My Contact Info", cardColor, textColor) { showContactDialog = true }
            Spacer(modifier = Modifier.height(12.dp))
            UserMenuOptionItem(Icons.Outlined.FavoriteBorder, "Favorites", cardColor, textColor) { onNavigateToFavorites() }
            Spacer(modifier = Modifier.height(12.dp))
            UserMenuOptionItem(Icons.Outlined.History, "Sold Items", cardColor, textColor) { onNavigateToHistory() }
            Spacer(modifier = Modifier.height(12.dp))

            // --- LOGOUT BUTTON (FIXED) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable {

                    // 1. Cuci Phone
                    try {
                        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
                        val securePrefs = EncryptedSharedPreferences.create(
                            context, "SecureCampusPrefs", masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )
                        securePrefs.edit().clear().commit()
                    } catch (e: Exception) { e.printStackTrace() }

                    // 2. LOGOUT SAHAJA (JANGAN KACAU DB)
                    authViewModel.logoutUser {
                        onLogout()
                    }
                }
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}
// Helper UI sama je...
@Composable
fun UserStatItem(count: String, label: String, textColor: Color, subTextColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = textColor)
        Text(label, color = subTextColor, fontSize = 12.sp)
    }
}

@Composable
fun UserMenuOptionItem(icon: ImageVector, title: String, cardColor: Color, textColor: Color, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = cardColor), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium, color = textColor, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
}