package com.utem.nougat.campuscycle.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    isDarkTheme: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    // --- STATE ---
    // Guna mutableStateListOf supaya perubahan UI nampak real-time
    var favoriteProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    // --- LOGIC WARNA ---
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6F8)
    val topBarColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    // --- 1. FETCH FAVORITES & PRODUCT DETAILS ---
    LaunchedEffect(Unit) {
        if (currentUser != null) {
            // Dengar perubahan pada collection 'favorites'
            db.collection("favorites")
                .whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        val productIds = snapshot.documents.mapNotNull { it.getString("productId") }

                        if (productIds.isEmpty()) {
                            favoriteProducts = emptyList()
                            isLoading = false
                            return@addSnapshotListener
                        }

                        // Tarik data Product terkini berdasarkan ID
                        // Kita guna 'toSet()' untuk elak duplicate ID kalau ada glitch
                        val uniqueIds = productIds.toSet().toList()
                        val tempProducts = mutableListOf<Product>()
                        var fetchCount = 0

                        uniqueIds.forEach { pid ->
                            db.collection("products").document(pid).get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        val product = Product(
                                            id = doc.id,
                                            title = doc.getString("title") ?: "Unknown",
                                            price = doc.getString("price") ?: "0.00",
                                            imgUrl = doc.getString("imgUrl") ?: "",
                                            category = doc.getString("category") ?: "",
                                            condition = doc.getString("condition") ?: "",
                                            sellerAuthId = doc.getString("sellerAuthId") ?: ""
                                        )
                                        tempProducts.add(product)
                                    }
                                    fetchCount++
                                    // Bila semua data produk dah ditarik, baru update UI sekali harung
                                    if (fetchCount == uniqueIds.size) {
                                        favoriteProducts = tempProducts
                                        isLoading = false
                                    }
                                }
                                .addOnFailureListener {
                                    fetchCount++ // Tetap increment kalau fail supaya loading stop
                                    if (fetchCount == uniqueIds.size) isLoading = false
                                }
                        }
                    } else {
                        favoriteProducts = emptyList()
                        isLoading = false
                    }
                }
        } else {
            isLoading = false
        }
    }

    // --- 2. LOGIC REMOVE (OPTIMISTIC UPDATE) ---
    fun removeFavorite(productId: String) {
        if (currentUser == null) return

        // A. UPDATE UI TERUS (Instant Feedback)
        // Kita buang item dari list screen DULU sebelum bagitahu server.
        // Ini buat app rasa laju gila.
        val oldList = favoriteProducts
        favoriteProducts = favoriteProducts.filter { it.id != productId }

        Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()

        // B. DELETE DARI DATABASE (Background Process)
        db.collection("favorites")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("productId", productId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    db.collection("favorites").document(document.id).delete()
                }
            }
            .addOnFailureListener {
                // C. KALAU FAIL (Jarang berlaku), letak balik item tu (Rollback)
                favoriteProducts = oldList
                Toast.makeText(context, "Failed to remove. Internet error.", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Favorites", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = textColor,
                    navigationIconContentColor = textColor
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampusBlue)
            }
        } else if (favoriteProducts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FavoriteBorder, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No favorites yet.", color = Color.Gray)
                    Text("Go explore and save items you love!", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(favoriteProducts, key = { it.id }) { product ->
                    // Reuse ProductCard (Pastikan pass isFavorite = true)
                    // Bila klik onFavoriteClick, kita panggil removeFavorite
                    ProductCard(
                        product = product,
                        isDark = isDarkTheme,
                        isFavorite = true, // Kat page ni semua memang favorite
                        onClick = { onNavigateToDetail("product_detail/${product.id}") },
                        onFavoriteClick = { removeFavorite(product.id) }
                    )
                }
            }
        }
    }
}