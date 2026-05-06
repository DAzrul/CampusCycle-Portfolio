package com.utem.nougat.campuscycle.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminFacultyMonitoringPage(isDark: Boolean, onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()

    // Senarai Fakulti UTeM
    val faculties = listOf("FTMK", "FKE", "FKEKK", "FKM", "FKP", "FPTT", "FTK")
    var selectedFaculty by remember { mutableStateOf("FTMK") }
    var productList by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val textColor = if (isDark) Color.White else Color.Black
    val backgroundColor = if (isDark) Color(0xFF121212) else Color(0xFFF5F6F8)
    val cardColor = if (isDark) Color(0xFF1E1E1E) else Color.White

    // --- FETCH DATA IKUT FAKULTI (LIVE SNAPSHOT) ---
    DisposableEffect(selectedFaculty, refreshTrigger) {
        isLoading = true
        val registration = db.collection("products")
            .whereEqualTo("sellerFaculty", selectedFaculty) // Logic filter
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    productList = snapshot.documents.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
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
                delay(1000)
                isRefreshing = false
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$selectedFaculty Items", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().pullRefresh(pullRefreshState)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

                // --- FACULTY SELECTOR (HORIZONTAL CHIPS) ---
                Text("Select Faculty to Monitor:", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(faculties) { faculty ->
                        val isSelected = selectedFaculty == faculty
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFaculty = faculty },
                            label = { Text(faculty) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CampusBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- LISTING MONITORING ---
                if (isLoading && !isRefreshing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CampusBlue)
                    }
                } else if (productList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items found for $selectedFaculty.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                        items(productList) { item ->
                            MonitoringCard(item, cardColor, textColor, onDelete = {
                                db.collection("products").document(item["id"].toString()).delete()
                            })
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
}

@Composable
fun MonitoringCard(item: Map<String, Any>, cardColor: Color, textColor: Color, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item["imgUrl"],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item["title"].toString(), fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                Text("RM${item["price"]}", color = CampusBlue, fontWeight = FontWeight.ExtraBold)
                Text("Matric No: ${item["sellerId"]}", fontSize = 11.sp, color = Color.Gray)

                // Badge Fakulti sebiji dlm mockup
                Surface(
                    color = Color(0xFF2ECC71).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = item["sellerFaculty"].toString(),
                        color = Color(0xFF27AE60),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Pancung Barang", tint = Color.Red)
            }
        }
    }
}
