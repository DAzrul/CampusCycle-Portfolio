package com.utem.nougat.campuscycle.ui.user

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailItemScreen(
    isDarkTheme: Boolean,
    productId: String,
    onNavigateBack: () -> Unit
) {
    // --- STATE DATA ---
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }
    var imgUrl by remember { mutableStateOf("") }
    var sellerName by remember { mutableStateOf("") }
    var sellerEmail by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var sellerQrUrl by remember { mutableStateOf("") }
    var sellerAuthId by remember { mutableStateOf("") } // <--- SIMPAN ID SELLER
    var status by remember { mutableStateOf("") } // <--- SIMPAN STATUS BARANG

    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) } // <--- STATE LOADING TRANSAKSI
    var showQrDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    // --- LOGIC DARK MODE ---
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6F8)
    val cardColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryTextColor = if (isDarkTheme) Color.LightGray else Color.Gray
    val qrImageBg = if (isDarkTheme) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)

    // --- FETCH DATA ---
    LaunchedEffect(productId) {
        db.collection("products").document(productId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    title = document.getString("title") ?: "Unknown Item"
                    price = document.getString("price") ?: "0.00"
                    description = document.getString("description") ?: "No description"
                    condition = document.getString("condition") ?: "Used"
                    imgUrl = document.getString("imgUrl") ?: ""
                    sellerName = document.getString("sellerName") ?: "Unknown Seller"
                    sellerEmail = document.getString("sellerEmail") ?: ""
                    status = document.getString("status") ?: "available"
                    sellerAuthId = document.getString("sellerAuthId") ?: ""

                    if (sellerAuthId.isNotEmpty()) {
                        db.collection("users").document(sellerAuthId).get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    sellerPhone = userDoc.getString("phoneNumber") ?: ""
                                    sellerQrUrl = userDoc.getString("qrCodeUrl") ?: ""
                                }
                            }
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Error loading details", Toast.LENGTH_SHORT).show()
            }
    }

    // --- TRANSACTION LOGIC (Mark as Sold + Notification) ---
    fun markAsSold() {
        isProcessing = true
        val transactionData = hashMapOf(
            "productId" to productId,
            "productTitle" to title,
            "sellerId" to currentUser?.uid,
            "amount" to (price.toDoubleOrNull() ?: 0.0),
            "timestamp" to System.currentTimeMillis()
        )

        // 1. Simpan dalam collection transactions
        db.collection("transactions").add(transactionData)
            .addOnSuccessListener {
                // 2. Update status produk dalam collection products
                db.collection("products").document(productId).update("status", "sold")
                    .addOnSuccessListener {
                        status = "sold"

                        // --- START NOTI LOGIC (SEBIJI MACAM MYLISTING) ---
                        db.collection("favorites")
                            .whereEqualTo("productId", productId)
                            .get()
                            .addOnSuccessListener { favoriteDocs ->
                                if (!favoriteDocs.isEmpty) {
                                    val batch = db.batch() // Guna batch biar laju mampus!

                                    for (doc in favoriteDocs) {
                                        val fanUserId = doc.getString("userId") ?: continue

                                        // Jangan hantar noti kat diri sendiri kalau seller pun like barang sendiri
                                        if (fanUserId == currentUser?.uid) continue

                                        val notiRef = db.collection("notifications").document()
                                        val soldNoti = hashMapOf(
                                            "recipientId" to fanUserId,
                                            "senderId" to (currentUser?.uid ?: "system"),
                                            "title" to "Item Sold Out! 💨",
                                            "message" to "Barang '${title}' yang anda sukai telah pun dijual kepada orang lain.",
                                            "type" to "sold_out",
                                            "productId" to productId,
                                            "timestamp" to System.currentTimeMillis(),
                                            "read" to false
                                        )
                                        batch.set(notiRef, soldNoti)
                                    }
                                    batch.commit() // Hantar semua sekali jalan
                                }
                                isProcessing = false
                                Toast.makeText(context, "Item Sold & Fans Notified!", Toast.LENGTH_LONG).show()
                            }
                    }
                    .addOnFailureListener {
                        isProcessing = false
                        Toast.makeText(context, "Failed to update status.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                isProcessing = false
                Toast.makeText(context, "Failed to record transaction.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- CONTACT LOGIC (WhatsApp) ---
    fun contactSeller() {
        if (sellerPhone.isNotEmpty()) {
            try {
                var rawPhone = sellerPhone.replace("+", "").replace(" ", "").replace("-", "")
                if (rawPhone.startsWith("0")) rawPhone = "6$rawPhone"
                val message = Uri.encode("Hi $sellerName, I'm interested in your item ($title) on CampusCycle.")
                val url = "https://api.whatsapp.com/send?phone=$rawPhone&text=$message"
                val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_SHORT).show()
            }
        } else if (sellerEmail.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$sellerEmail")
                putExtra(Intent.EXTRA_SUBJECT, "Inquiry: $title")
            }
            context.startActivity(intent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listing Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cardColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->

        // --- POPUP DIALOG QR CODE ---
        if (showQrDialog && sellerQrUrl.isNotEmpty()) {
            Dialog(onDismissRequest = { showQrDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("Scan to WhatsApp", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                        Spacer(modifier = Modifier.height(16.dp))

                        AsyncImage(
                            model = sellerQrUrl,
                            contentDescription = "Seller QR Code",
                            modifier = Modifier.size(250.dp).clip(RoundedCornerShape(8.dp)).background(qrImageBg),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { showQrDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = CampusBlue)) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampusBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. IMAGE PRODUCT
                Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color.Gray)) {
                    AsyncImage(model = imgUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())

                    // Banner Sold Out (Muncul kalau dah dijual)
                    if (status == "sold") {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(color = Color.Red, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "SOLD OUT",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                    fontSize = 24.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. MAIN INFO
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f))
                            Surface(color = if(isDarkTheme) Color(0xFF333333) else Color(0xFFEEEEEE), shape = RoundedCornerShape(4.dp)) {
                                Text(condition, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("RM$price", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CampusBlue)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(description, fontSize = 14.sp, color = secondaryTextColor, lineHeight = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. SELLER INFO & MARK AS SOLD (Fucking logic starts here)
                val isOwner = currentUser?.uid != null && sellerAuthId == currentUser.uid

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(if(isDarkTheme) Color.DarkGray else Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (sellerName.isNotEmpty()) sellerName.take(1).uppercase() else "S", fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sellerName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textColor)
                                Text(if(isOwner) "You are the Seller" else "Seller", fontSize = 12.sp, color = secondaryTextColor)
                            }
                            Row {
                                if (sellerQrUrl.isNotEmpty() && !isOwner) {
                                    IconButton(onClick = { showQrDialog = true }) {
                                        Icon(Icons.Default.QrCode, contentDescription = "Show QR", tint = textColor)
                                    }
                                }
                                if (!isOwner && status != "sold") {
                                    TextButton(onClick = { contactSeller() }) {
                                        Text(
                                            text = if (sellerPhone.isNotEmpty()) "WhatsApp" else "Contact",
                                            color = if (sellerPhone.isNotEmpty()) Color(0xFF25D366) else CampusBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // --- BUTANG MARK AS SOLD (KHAS UNTUK SELLER SAHAJA) ---
                        if (isOwner && status == "available") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { markAsSold() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                enabled = !isProcessing
                            ) {if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp), // Set size guna modifier, bukan parameter direct!
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Mark as Sold", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            }
                            Text(
                                "Recording this will update student savings analytics.",
                                fontSize = 10.sp,
                                color = secondaryTextColor,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}