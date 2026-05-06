package com.utem.nougat.campuscycle.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import com.google.firebase.firestore.WriteBatch

// Data Model Notifikasi (Tambah field 'read')
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val timestamp: Long,
    val isRead: Boolean // <--- Kita guna ni untuk UI
)

@Composable
fun NotificationScreen(
    isDarkTheme: Boolean,
    onSettingsClick: () -> Unit
) {
    // --- STATE ---
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // --- THEME COLORS ---
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6F8)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cardColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    // Warna khas untuk noti yang BELUM BACA (Biru cair sikit)
    val unreadCardColor = if (isDarkTheme) Color(0xFF2C2C3E) else Color(0xFFE3F2FD)
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    // --- FETCH DATA (LIVE) ---
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("notifications")
                .whereEqualTo("recipientId", currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val items = snapshot.documents.map { doc ->
                            NotificationItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "Notification",
                                message = doc.getString("message") ?: "",
                                type = doc.getString("type") ?: "system",
                                timestamp = doc.getLong("timestamp") ?: 0L,
                                isRead = doc.getBoolean("read") ?: false // Ambil status read
                            )
                        }
                        notifications = items
                        isLoading = false

                        // --- AUTO MARK AS READ ---
                        // Bila user dah nampak list ni, kita update database
                        // Bagitahu "Semua noti ni kira dah dibaca"
                        items.forEach { item ->
                            if (!item.isRead) {
                                db.collection("notifications").document(item.id)
                                    .update("read", true)
                            }
                        }
                    }
                }
        } else {
            isLoading = false
        }
    }

    // --- DELETE FUNCTION ---
    fun deleteNotification(id: String) {
        // Padam kat database
        db.collection("notifications").document(id).delete()
    }

    // --- CLEAR ALL FUNCTION ---
    fun clearAllNotifications() {
        if (currentUser == null) return
        
        db.collection("notifications")
            .whereEqualTo("recipientId", currentUser.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show()
                }
            }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Notifications") },
            text = { Text("Are you sure you want to clear all your notifications? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    clearAllNotifications()
                    showClearDialog = false
                }) {
                    Text("Clear All", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notifications",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Row {
                if (notifications.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = Color.Red)
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = textColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- LIST CONTENT ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampusBlue)
            }
        } else if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Notifications, null, tint = subTextColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No notifications yet.", color = subTextColor)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(
                        notification = notification,
                        // Kalau belum baca, guna warna lain supaya user perasan
                        cardColor = if (notification.isRead) cardColor else unreadCardColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onDelete = { deleteNotification(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    onDelete: () -> Unit
) {
    // Tentukan Icon & Warna berdasarkan Type yang baru
    val icon = when (notification.type) {
        "favorite" -> Icons.Default.Favorite
        "sold_out" -> Icons.Default.Block
        "price_drop" -> Icons.Default.TrendingDown 
        "system" -> Icons.Default.Campaign     
        "order" -> Icons.Default.ShoppingBag
        else -> Icons.Default.Notifications
    }

    val iconColor = when (notification.type) {
        "favorite" -> Color(0xFFFF5252)
        "sold_out" -> Color(0xFF757575)
        "price_drop" -> Color(0xFF00BCD4) // Warna Cyan
        "system" -> Color(0xFFFF9800)     // Warna Orange
        "order" -> Color(0xFF4CAF50)
        else -> CampusBlue
    }

    // Kalau belum baca, tajuk jadi BOLD
    val titleWeight = if (notification.isRead) FontWeight.Bold else FontWeight.ExtraBold

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        fontWeight = titleWeight,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    // Dot indicator kalau belum baca
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.message,
                    fontSize = 13.sp,
                    color = subTextColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.align(Alignment.Bottom)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Delete",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onDelete() }
                )
            }
        }
    }
}
