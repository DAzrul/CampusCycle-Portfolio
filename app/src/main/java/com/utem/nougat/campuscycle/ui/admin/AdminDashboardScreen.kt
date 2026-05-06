package com.utem.nougat.campuscycle.ui.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import com.google.firebase.messaging.FirebaseMessaging

// --- DATA MODEL ---
data class AdminPendingItem(
    val id: String,
    val title: String,
    val price: String,
    val imgUrl: String,
    val sellerName: String,
    val category: String,
    val faculty: String
)

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToBroadcast: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showMonitoringPage by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val navColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    // --- FETCH PROFILE IMAGE & FCM TOKEN UPDATE ---
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // 1. Listen Profile Image
            db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    profileImageUrl = snapshot?.getString("photoUrl")
                }

            // 2. FORCE UPDATE FCM TOKEN (Pancung Noti Sesat)
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                db.collection("users").document(currentUser.uid)
                    .update("fcmToken", token)
            }
        }
    }

    if (showMonitoringPage) {
        // Pastikan kau dah buat file AdminFacultyMonitoringPage.kt ni!
        AdminFacultyMonitoringPage(isDark = isDark, onBack = { showMonitoringPage = false })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = navColor, tonalElevation = 8.dp) {
                    AdminNavItem("Dashboard", selectedTab == 0, { selectedTab = 0 }, Icons.Filled.Home, Icons.Outlined.Home)
                    AdminNavItem("Report", selectedTab == 1, { selectedTab = 1 }, Icons.Filled.PieChart, Icons.Outlined.PieChart)
                    AdminNavItem(
                        label = "Profile",
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        sIcon = Icons.Filled.AccountCircle,
                        uIcon = Icons.Outlined.AccountCircle,
                        profileUrl = profileImageUrl // <--- HANTAR DATA GAMBAR!
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> AdminDashboardContent(
                        onNavigateToBroadcast = onNavigateToBroadcast,
                        onNavigateToMonitor = { showMonitoringPage = true }
                    )
                    1 -> AdminAnalyticsPage()
                    2 -> ProfileAdminPage(
                        onLogout = onLogout,
                        onEditProfile = onEditProfile,
                        onSettingsClick = onSettingsClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminDashboardContent(
    onNavigateToBroadcast: () -> Unit,
    onNavigateToMonitor: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black
    val subTextColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    var pendingList by remember { mutableStateOf<List<AdminPendingItem>>(emptyList()) }
    var totalUsers by remember { mutableLongStateOf(0L) }
    var totalItems by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFaculty by remember { mutableStateOf<String?>(null) }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("users").count().get(AggregateSource.SERVER).addOnSuccessListener { totalUsers = it.count }
        db.collection("products").count().get(AggregateSource.SERVER).addOnSuccessListener { totalItems = it.count }
    }

    LaunchedEffect(selectedFaculty) {
        isLoading = true
        var query: Query = db.collection("products").whereEqualTo("status", "pending")
        if (selectedFaculty != null) query = query.whereEqualTo("sellerFaculty", selectedFaculty)

        query.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                pendingList = snapshot.documents.map { doc ->
                    AdminPendingItem(
                        id = doc.id,
                        title = doc.getString("title") ?: "Unknown",
                        price = doc.getString("price") ?: "0.00",
                        imgUrl = doc.getString("imgUrl") ?: "",
                        sellerName = doc.getString("sellerName") ?: "Unknown",
                        category = doc.getString("category") ?: "-",
                        faculty = doc.getString("sellerFaculty") ?: "N/A"
                    )
                }
            }
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(20.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ShoppingCart, null, tint = CampusBlue, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("CampusCycle Admin", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- BROADCAST CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToBroadcast() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CampusBlue),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, null, tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Broadcast Notification", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Send message to all users", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- FACULTY MONITORING CARD (NEW) ---
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onNavigateToMonitor() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2ECC71)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Analytics, null, tint = Color.White, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Faculty Monitoring", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Monitor items by FTMK, FKE, etc.", color = Color.White.copy(0.8f), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AdminStatBox("Users", "$totalUsers", Modifier.weight(1f), cardColor, textColor, subTextColor)
            AdminStatBox("Items", "$totalItems", Modifier.weight(1f), cardColor, textColor, subTextColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Filter by Faculty", fontWeight = FontWeight.SemiBold, color = textColor)
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val faculties = listOf("FKE", "FKEKK", "FTMK", "FKM", "FKP", "FPTT", "FTK")
            faculties.forEach { faculty ->
                val isSelected = selectedFaculty == faculty
                AdminFacultyChip(faculty, isSelected, { selectedFaculty = if (isSelected) null else faculty }, cardColor, textColor)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(text = if (selectedFaculty == null) "Pending Approvals" else "Pending ($selectedFaculty)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CampusBlue) }
        } else if (pendingList.isEmpty()) {
            Text("No item pending.", color = subTextColor, modifier = Modifier.padding(vertical = 20.dp))
        } else {
            pendingList.forEach { item ->
                AdminItemCard(item, cardColor, textColor, db)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun AdminItemCard(item: AdminPendingItem, cardColor: Color, textColor: Color, db: FirebaseFirestore) {
    val context = LocalContext.current // FIX: Tambah context kat sini!

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = item.imgUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(65.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                Text("RM${item.price}", color = CampusBlue, fontWeight = FontWeight.Bold)
                Text("Fakulti: ${item.faculty}", color = Color.Gray, fontSize = 11.sp)
            }
            Row {
                IconButton(onClick = { db.collection("products").document(item.id).delete() }) {
                    Icon(Icons.Default.Close, null, tint = Color.Red)
                }
                IconButton(onClick = {
                    db.collection("products").document(item.id).update("status", "available")
                        .addOnSuccessListener {
                            // Logic Noti Peminat
                            db.collection("favorites").whereEqualTo("productId", item.id).get()
                                .addOnSuccessListener { favoriteDocs ->
                                    if (!favoriteDocs.isEmpty) {
                                        val batch = db.batch()
                                        for (doc in favoriteDocs) {
                                            val fanUserId = doc.getString("userId") ?: continue
                                            val notiRef = db.collection("notifications").document()
                                            batch.set(notiRef, hashMapOf(
                                                "recipientId" to fanUserId,
                                                "senderId" to "system",
                                                "title" to "Item Updated! ✨",
                                                "message" to "Item '${item.title}' now available!",
                                                "type" to "system",
                                                "productId" to item.id,
                                                "timestamp" to System.currentTimeMillis(),
                                                "read" to false
                                            ))
                                        }
                                        batch.commit()
                                    }
                                    Toast.makeText(context, "Approved & Fans Notified!", Toast.LENGTH_SHORT).show()
                                }
                        }
                }) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50))
                }
            }
        }
    }
}

// --- REST OF THE HELPERS (AdminNavItem, AdminStatBox, AdminProfilePage) KEKAL SAMA ---
@Composable
private fun RowScope.AdminNavItem(label: String, selected: Boolean, onClick: () -> Unit, sIcon: ImageVector, uIcon: ImageVector, profileUrl: String? = null) {
    NavigationBarItem(
        selected = selected, onClick = onClick,
        icon = {
            if (label == "Profile" && !profileUrl.isNullOrEmpty()) {
                AsyncImage(model = profileUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(26.dp).clip(CircleShape).then(if (selected) Modifier.border(2.dp, CampusBlue, CircleShape) else Modifier))
            } else {
                Icon(if (selected) sIcon else uIcon, null, modifier = Modifier.size(26.dp))
            }
        },
        label = { Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        colors = NavigationBarItemDefaults.colors(selectedIconColor = CampusBlue, indicatorColor = Color.Transparent)
    )
}

@Composable
private fun AdminFacultyChip(name: String, isSelected: Boolean, onClick: () -> Unit, cardColor: Color, textColor: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isSelected) CampusBlue else Color.LightGray),
        color = if (isSelected) CampusBlue else cardColor,
        modifier = Modifier.height(36.dp).clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else textColor)
        }
    }
}

@Composable
private fun AdminStatBox(title: String, count: String, modifier: Modifier, cardColor: Color, textColor: Color, subTextColor: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = subTextColor)
            Text(count, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}
