package com.utem.nougat.campuscycle.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

// Data Model
data class MyProductItem(
    val id: String,
    val title: String,
    val price: String,
    val imgUrl: String,
    val category: String,
    val status: String,
    val timestamp: Long
)

@Composable
fun MyListingScreen(
    isDarkTheme: Boolean,
    onNavigateToUpdate: (String) -> Unit = {},
    onSettingsClick: () -> Unit // <--- TAMBAH INI (Callback Settings)
) {
    // --- STATE ---
    var myProducts by remember { mutableStateOf<List<MyProductItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // --- LOGIC WARNA DARK MODE ---
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6F8)
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val cardColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val subTextColor = if (isDarkTheme) Color.LightGray else Color.Gray

    // --- FETCH DATA ---
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            db.collection("products")
                .whereEqualTo("sellerAuthId", currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val items = snapshot.documents.map { doc ->
                            MyProductItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "Untitled",
                                price = doc.getString("price") ?: "0.00",
                                imgUrl = doc.getString("imgUrl") ?: "",
                                category = doc.getString("category") ?: "-",
                                status = doc.getString("status") ?: "pending",
                                timestamp = doc.getLong("timestamp") ?: 0L
                            )
                        }.sortedByDescending { it.timestamp }

                        myProducts = items
                        isLoading = false
                    }
                }
        } else {
            isLoading = false
        }
    }

    fun markAsSold(itemId: String, itemTitle: String) {
        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        db.collection("products").document(itemId).update("status", "sold")
            .addOnSuccessListener {
                // CARI PEMINAT BARANG NI
                db.collection("favorites")
                    .whereEqualTo("productId", itemId)
                    .get()
                    .addOnSuccessListener { favoriteDocs ->
                        if (!favoriteDocs.isEmpty) {
                            val batch = db.batch()
                            for (doc in favoriteDocs) {
                                val fanUserId = doc.getString("userId") ?: continue
                                if (fanUserId == currentUser?.uid) continue // Jangan hantar kat diri sendiri

                                val notiRef = db.collection("notifications").document()
                                batch.set(notiRef, hashMapOf(
                                    "recipientId" to fanUserId,
                                    "senderId" to (currentUser?.uid ?: "system"),
                                    "title" to "Item Sold Out! 💨",
                                    "message" to "Barang '${itemTitle}' yang anda sukai telah dijual.",
                                    "type" to "sold_out", // <--- INI TYPE DIA
                                    "productId" to itemId,
                                    "timestamp" to System.currentTimeMillis(),
                                    "read" to false
                                ))
                            }
                            batch.commit() // Hantar semua sekali jalan!
                        }
                    }
            }
    }

    // --- DELETE FUNCTION ---
    fun deleteItem(itemId: String) {
        db.collection("products").document(itemId).delete()
            .addOnSuccessListener { Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show() }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Listing",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            // UPDATE SINI: Panggil callback bila tekan
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampusBlue)
            }
        } else if (myProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("You haven't listed anything yet.", color = subTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Go to 'Create' tab to sell something!", fontSize = 12.sp, color = subTextColor)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(myProducts) { product ->
                    MyListingCard(
                        product = product,
                        cardColor = cardColor,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onDelete = { deleteItem(product.id) },
                        onUpdate = { onNavigateToUpdate(product.id) },
                        onMarkAsSold = { markAsSold(product.id, product.title) } // <--- Pass function baru
                    )
                }
            }
        }
    }
}

@Composable
fun MyListingCard(
    product: MyProductItem,
    cardColor: Color,
    textColor: Color,
    subTextColor: Color,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    onMarkAsSold: () -> Unit
) {
    val isSold = product.status == "sold"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSold) cardColor.copy(alpha = 0.6f) else cardColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    // --- LOGIC BADGE STATUS (TAMBAH SOLD) ---
                    Surface(
                        color = when (product.status) {
                            "available" -> Color(0xFFE8F5E9) // Hijau
                            "sold" -> Color(0xFFFFEBEE)      // Merah Cair
                            else -> Color(0xFFFFF3E0)        // Orange (Pending)
                        },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = when (product.status) {
                                "available" -> "Active"
                                "sold" -> "Sold Out"
                                else -> "Pending Approval"
                            },
                            color = when (product.status) {
                                "available" -> Color(0xFF2E7D32)
                                "sold" -> Color(0xFFC62828)
                                else -> Color(0xFFEF6C00)
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = product.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isSold) textColor.copy(0.5f) else textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("RM${product.price}", fontWeight = FontWeight.ExtraBold, color = CampusBlue)
                }

                AsyncImage(
                    model = product.imgUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = subTextColor.copy(alpha = 0.1f))

            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                // Jangan bagi Update kalau dah Sold
                if (!isSold) {
                    TextButton(onClick = onMarkAsSold) {
                        Text("Mark as Sold", color = Color(0xFF2E7D32), fontSize = 13.sp)
                    }
                    TextButton(onClick = onUpdate) {
                        Text("Update", color = CampusBlue, fontSize = 13.sp)
                    }
                }

                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color.Red, fontSize = 13.sp)
                }
            }
        }
    }
}