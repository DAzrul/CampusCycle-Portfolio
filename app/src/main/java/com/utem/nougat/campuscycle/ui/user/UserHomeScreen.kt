package com.utem.nougat.campuscycle.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- DATA MODEL ---
data class Product(
    val id: String = "",
    val title: String = "Untitled",
    val price: String = "0.00",
    val imgUrl: String = "",
    val category: String = "",
    val condition: String = "",
    val sellerAuthId: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserHomeScreen(
    isDarkTheme: Boolean,
    initialTab: Int = 0,
    onNavigateToDetail: (String) -> Unit = {},
    onEditProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onLogout: () -> Unit
){
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    // --- CCTV SESSION MONITOR (VERSI PERFECT) ---
    // Guna DisposableEffect supaya listener mati bila kita logout/keluar screen
    DisposableEffect(currentUser) {
        if (currentUser != null) {
            val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val sharedPreferences = EncryptedSharedPreferences.create(
                context, "SecureCampusPrefs", masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // Pasang Telinga (Listener)
            val registration = db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, e ->
                    if (snapshot != null && snapshot.exists()) {
                        val remoteSessionId = snapshot.getString("currentSessionId") ?: ""
                        val localSessionId = sharedPreferences.getString("SESSION_ID", "") ?: ""

                        // 🔥 LOGIC PENYELAMAT 🔥
                        // 1. Kalau local kosong (baru login), JANGAN KICK.
                        // 2. Kalau remote kosong (baru logout), JANGAN KICK.
                        // 3. Hanya kick kalau dua-dua ada isi TAPI tak sama.
                        if (localSessionId.isNotEmpty() && remoteSessionId.isNotEmpty() && remoteSessionId != localSessionId) {

                            // Padam data dalam phone
                            sharedPreferences.edit().clear().apply()
                            FirebaseAuth.getInstance().signOut()

                            // Tendang keluar
                            onLogout()
                            Toast.makeText(context, "Logged in on another device! Force Logout.", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            // Wajib: Matikan listener bila function ini tamat (logout/tukar screen)
            onDispose { registration.remove() }
        } else {
            onDispose { }
        }
    }

    // --- 1. FETCH PROFILE IMAGE ---
    DisposableEffect(currentUser) {
        if (currentUser != null) {
            val registration = db.collection("users").document(currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    profileImageUrl = snapshot?.getString("photoUrl")
                }
            onDispose { registration.remove() }
        } else {
            onDispose { }
        }
    }

    // --- 2. UPDATE FCM TOKEN ---
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                db.collection("users").document(currentUser.uid).update("fcmToken", token)
            }
        }
    }

    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6F8)
    val bottomBarColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White

    Scaffold(
        containerColor = backgroundColor,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> MarketplaceContent(isDark = isDarkTheme, onNavigateToDetail = onNavigateToDetail)
                    1 -> NotificationScreen(isDarkTheme = isDarkTheme, onSettingsClick = onSettingsClick)
                    2 -> CreateListingScreen(isDarkTheme = isDarkTheme, onPostSuccess = { selectedTab = 0 }, onCancel = { selectedTab = 0 })
                    3 -> MyListingScreen(isDarkTheme = isDarkTheme, onNavigateToUpdate = { id -> onNavigateToDetail("update_listing/$id") }, onSettingsClick = onSettingsClick)
                    4 -> UserProfileScreen(onLogout = onLogout, onEditProfile = onEditProfile, onSettingsClick = onSettingsClick, onNavigateToMyListings = { selectedTab = 3 }, onNavigateToFavorites = onNavigateToFavorites, onNavigateToHistory = onNavigateToHistory)
                }
            }

            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    containerColor = bottomBarColor,
                    profileUrl = profileImageUrl
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MarketplaceContent(
    isDark: Boolean,
    onNavigateToDetail: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var productList by remember { mutableStateOf<List<Product>>(emptyList()) }
    var favoritedProductIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val categories = listOf(
        "Textbooks & Study Materials",
        "Electronics & Gadget",
        "Dorm Essential & Furniture",
        "Clothing & Accesories",
        "Kitchen & Appliances",
        "Hobbies & Sports",
        "Tools & Equipment",
        "Others"
    )
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    val textColor = if (isDark) Color.White else Color.Black
    val searchBarBg = if (isDark) Color(0xFF1E1E1E) else Color.White

    // --- MARKETPLACE LISTENER ---
    DisposableEffect(selectedCategory, refreshTrigger) {
        isLoading = true
        var query = db.collection("products")
            .whereEqualTo("status", "available")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        if (selectedCategory != null) query = query.whereEqualTo("category", selectedCategory)

        val registration = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                isLoading = false
                isRefreshing = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                productList = snapshot.documents.map { doc ->
                    Product(
                        id = doc.id,
                        title = doc.getString("title") ?: "No Title",
                        price = doc.getString("price") ?: "0.00",
                        imgUrl = doc.getString("imgUrl") ?: "",
                        sellerAuthId = doc.getString("sellerAuthId") ?: ""
                    )
                }
            }
            isLoading = false
            isRefreshing = false
        }
        onDispose { registration.remove() }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                refreshTrigger++
                // Give it a little delay for better UX if the data is already cached
                delay(1000)
                isRefreshing = false
            }
        }
    )

    // --- FAVORITES LISTENER ---
    DisposableEffect(currentUser) {
        if (currentUser != null) {
            val reg = db.collection("favorites").whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        favoritedProductIds = snapshot.documents.mapNotNull { it.getString("productId") }.toSet()
                    }
                }
            onDispose { reg.remove() }
        } else {
            onDispose { }
        }
    }

    fun toggleFavorite(product: Product) {
        if (currentUser == null) return
        val isFavorite = favoritedProductIds.contains(product.id)

        if (isFavorite) {
            db.collection("favorites")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("productId", product.id)
                .get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot) db.collection("favorites").document(doc.id).delete()
                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show()
                }
        } else {
            val favData = hashMapOf("userId" to currentUser.uid, "productId" to product.id, "timestamp" to System.currentTimeMillis())
            db.collection("favorites").add(favData).addOnSuccessListener {
                Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
                if (product.sellerAuthId.isNotEmpty() && product.sellerAuthId != currentUser.uid) {
                    val notiData = hashMapOf(
                        "recipientId" to product.sellerAuthId,
                        "senderId" to currentUser.uid,
                        "title" to "New Favorite!",
                        "message" to "Someone liked your item: ${product.title}",
                        "type" to "favorite",
                        "productId" to product.id,
                        "timestamp" to System.currentTimeMillis(),
                        "read" to false
                    )
                    db.collection("notifications").add(notiData)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("CampusCycle", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search items...") },
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(50)),
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(focusedContainerColor = searchBarBg, unfocusedContainerColor = searchBarBg, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories.size) { index ->
                    val cat = categories[index]
                    val selected = cat == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) CampusBlue else (if (isDark) Color(0xFF333333) else Color.White),
                        modifier = Modifier.clickable { selectedCategory = if (selected) null else cat }
                    ) {
                        Text(cat, color = if (selected) Color.White else textColor, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && !isRefreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CampusBlue) }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                    val filteredList = productList.filter { it.title.contains(searchQuery, true) }
                    items(filteredList.size) { index ->
                        val prod = filteredList[index]
                        ProductCard(prod, isDark, favoritedProductIds.contains(prod.id),
                            onClick = { onNavigateToDetail("product_detail/${prod.id}") },
                            onFavoriteClick = { toggleFavorite(prod) }
                        )
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            backgroundColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
            contentColor = CampusBlue
        )
    }
}

@Composable
fun ProductCard(product: Product, isDark: Boolean, isFavorite: Boolean, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = if(isDark) Color(0xFF1E1E1E) else Color.White)) {
        Column {
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                AsyncImage(model = product.imgUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                IconButton(onClick = onFavoriteClick, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(0.4f), CircleShape)) {
                    Icon(if(isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if(isFavorite) Color.Red else Color.White)
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(product.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if(isDark) Color.White else Color.Black)
                Text("RM${product.price}", color = CampusBlue, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun BottomNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit, containerColor: Color, profileUrl: String?) {
    NavigationBar(containerColor = containerColor) {
        val items = listOf(
            Triple(Icons.Outlined.Home, Icons.Filled.Home, "Marketplace"),
            Triple(Icons.Outlined.Notifications, Icons.Filled.Notifications, "Notification"),
            Triple(Icons.Outlined.AddCircleOutline, Icons.Filled.AddCircle, "Create"),
            Triple(Icons.Outlined.Inventory2, Icons.Filled.Inventory2, "My Listing"),
            Triple(Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle, "Profile")
        )

        items.forEachIndexed { index, item ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    if (index == 4 && !profileUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = profileUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(26.dp).clip(CircleShape).then(if (isSelected) Modifier.border(2.dp, CampusBlue, CircleShape) else Modifier)
                        )
                    } else {
                        Icon(if (isSelected) item.second else item.first, item.third, modifier = Modifier.size(26.dp))
                    }
                },
                label = { Text(item.third, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = CampusBlue, indicatorColor = Color.Transparent)
            )
        }
    }
}
