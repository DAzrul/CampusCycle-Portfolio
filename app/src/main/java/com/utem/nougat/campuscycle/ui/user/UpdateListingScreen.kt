package com.utem.nougat.campuscycle.ui.user

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateListingScreen(
    isDarkTheme: Boolean,
    productId: String,
    onNavigateBack: () -> Unit,
    onUpdateSuccess: () -> Unit
) {
    // --- STATE ---
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var oldPrice by remember { mutableStateOf("") } // <--- SIMPAN HARGA LAMA
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf("") }
    var existingImgUrl by remember { mutableStateOf("") }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    val categories = listOf("Textbooks", "Electronics", "Furniture", "Apparel", "Stationery")
    val conditions = listOf("Like New", "Good", "Fair")
    var expandedCategory by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // --- LOGIC DARK MODE ---
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6F8)
    val cardColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val borderColor = if (isDarkTheme) Color.Gray else Color(0xFFE0E0E0)

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedBorderColor = CampusBlue,
        unfocusedBorderColor = borderColor,
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        cursorColor = CampusBlue
    )

    // --- FETCH DATA ---
    LaunchedEffect(productId) {
        db.collection("products").document(productId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    title = document.getString("title") ?: ""
                    val currentPrice = document.getString("price") ?: ""
                    price = currentPrice
                    oldPrice = currentPrice // Simpan harga asal untuk banding nanti
                    description = document.getString("description") ?: ""
                    selectedCategory = document.getString("category") ?: categories[0]
                    selectedCondition = document.getString("condition") ?: conditions[0]
                    existingImgUrl = document.getString("imgUrl") ?: ""
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Failed to load product", Toast.LENGTH_SHORT).show()
            }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> newImageUri = uri }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Listing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampusBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Text("Item Title *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = inputColors
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Price (RM) *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        OutlinedTextField(
                            value = price,
                            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) price = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = inputColors
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Category *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        ExposedDropdownMenuBox(
                            expanded = expandedCategory,
                            onExpandedChange = { expandedCategory = !expandedCategory }
                        ) {
                            OutlinedTextField(
                                value = selectedCategory,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                colors = inputColors
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCategory,
                                onDismissRequest = { expandedCategory = false },
                                modifier = Modifier.background(cardColor)
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category, color = textColor) },
                                        onClick = {
                                            selectedCategory = category
                                            expandedCategory = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Condition *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            conditions.forEach { condition ->
                                val isSelected = condition == selectedCondition
                                val btnColor = if (isSelected) CampusBlue else Color.Transparent
                                val txtColor = if (isSelected) Color.White else CampusBlue
                                val border = if (isSelected) null else BorderStroke(1.dp, borderColor)

                                Button(
                                    onClick = { selectedCondition = condition },
                                    colors = ButtonDefaults.buttonColors(containerColor = btnColor),
                                    border = border,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(condition, color = txtColor, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Description", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            colors = inputColors
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Upload Images *", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (newImageUri != null) {
                                AsyncImage(model = newImageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                            } else if (existingImgUrl.isNotEmpty()) {
                                AsyncImage(model = existingImgUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))
                            } else {
                                Icon(Icons.Default.Add, null, tint = borderColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                isUpdating = true
                                updateProduct(
                                    context, productId, title, price, oldPrice, // <--- PASS HARGA LAMA
                                    selectedCategory, selectedCondition, description, existingImgUrl, newImageUri,
                                    onSuccess = {
                                        isUpdating = false
                                        Toast.makeText(context, "Product Updated!", Toast.LENGTH_SHORT).show()
                                        onUpdateSuccess()
                                    },
                                    onFail = {
                                        isUpdating = false
                                        Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CampusBlue),
                            enabled = !isUpdating
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Update Listing", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- UPDATE LOGIC DENGAN PRICE DROP NOTIFICATION ---
fun updateProduct(
    context: android.content.Context,
    productId: String,
    title: String,
    price: String,
    oldPrice: String, // <--- TERIMA HARGA LAMA
    category: String,
    condition: String,
    desc: String,
    oldImgUrl: String,
    newImageUri: Uri?,
    onSuccess: () -> Unit,
    onFail: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val productRef = db.collection("products").document(productId)

    val updates = hashMapOf<String, Any>(
        "title" to title,
        "price" to price,
        "category" to category,
        "condition" to condition,
        "description" to desc,
        "status" to "pending"
    )

    // --- DALAM UpdateListingScreen.kt ---
    fun completeUpdate() {
        productRef.update(updates)
            .addOnSuccessListener {
                // Kita tak hantar noti kat sini lagi.
                // Kita tunggu Admin approve baru noti keluar.
                onSuccess()
            }
            .addOnFailureListener { onFail(it.message ?: "Update failed") }
    }

    if (newImageUri != null) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileName = "product_images/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(fileName)

        imageRef.putFile(newImageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    updates["imgUrl"] = uri.toString()
                    completeUpdate()
                }
            }
            .addOnFailureListener { onFail("Image upload failed") }
    } else {
        completeUpdate()
    }
}