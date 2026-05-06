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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    isDarkTheme: Boolean,
    onPostSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    // --- STATE FORM ---
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

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
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var expandedCategory by remember { mutableStateOf(false) }

    val conditions = listOf("Like New", "Good", "Fair")
    var selectedCondition by remember { mutableStateOf("Like New") }

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // --- STATE UTK USER INFO ---
    var currentSellerName by remember { mutableStateOf("Loading...") }
    var currentStudentId by remember { mutableStateOf("") }

    val context = LocalContext.current

    // --- 1. FETCH INFO USER (DISPLAY) ---
    LaunchedEffect(Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        // FIX: Cuba cari 'fullName' dulu, kalau takde baru 'name'
                        val nameFromDb = doc.getString("fullName") ?: doc.getString("name")
                        // Kalau database kosong, ambil dari FirebaseAuth Profile, kalau takde jugak 'Unknown'
                        currentSellerName = nameFromDb ?: user.displayName ?: "Unknown User"

                        currentStudentId = doc.getString("studentId") ?: "No ID"
                    }
                }
        } else {
            currentSellerName = "Guest (Error)"
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> imageUri = uri }
    )

    // --- WARNA UI ---
    val containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
    val backgroundColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6F8)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Create Listing", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ... dlm Column CreateListingScreen ...
        Card(
            colors = CardDefaults.cardColors(containerColor = CampusBlue.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = CampusBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Posting as:", fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))

                    // Tunjuk Fakulti skali kat sini
                    val userFaculty = getFacultyFromId(currentStudentId)
                    Text("$currentSellerName | $userFaculty", fontWeight = FontWeight.Bold, color = CampusBlue)
                    Text(currentStudentId, fontSize = 11.sp, color = CampusBlue.copy(0.7f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // FORM CARD UTAMA
        Card(
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {

                // ITEM TITLE
                InputLabel("Item Title *", textColor)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g., Engineering Book", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = inputColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // PRICE
                InputLabel("Price (RM) *", textColor)
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) price = it },
                    placeholder = { Text("0.00", color = Color.Gray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = inputColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // CATEGORY
                InputLabel("Category *", textColor)
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
                        shape = RoundedCornerShape(8.dp),
                        colors = inputColors
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false },
                        modifier = Modifier.background(containerColor)
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

                Spacer(modifier = Modifier.height(20.dp))

                // CONDITION
                InputLabel("Condition *", textColor)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    conditions.forEach { condition ->
                        val isSelected = condition == selectedCondition
                        val btnBg = if (isSelected) CampusBlue else Color.Transparent
                        val btnText = if (isSelected) Color.White else CampusBlue
                        val btnBorder = if (isSelected) null else BorderStroke(1.dp, borderColor)

                        Button(
                            onClick = { selectedCondition = condition },
                            colors = ButtonDefaults.buttonColors(containerColor = btnBg),
                            border = btnBorder,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(condition, color = btnText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // DESCRIPTION
                InputLabel("Description", textColor)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Describe your items...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = inputColors
                )

                Spacer(modifier = Modifier.height(20.dp))

                // UPLOAD IMAGE
                InputLabel("Upload Images *", textColor)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(0.6f), CircleShape)
                                .size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to upload Images", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // SUBMIT BUTTON
                Button(
                    onClick = {
                        if (title.isEmpty() || price.isEmpty() || imageUri == null) {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        uploadListing(
                            context, title, price, selectedCategory, selectedCondition, description, imageUri!!,
                            onSuccess = {
                                isLoading = false
                                Toast.makeText(context, "Submitted! Waiting for Admin Approval.", Toast.LENGTH_LONG).show()
                                onPostSuccess()
                            },
                            onFail = {
                                isLoading = false
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CampusBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Submit for Review", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(200.dp))
    }
}

@Composable
fun InputLabel(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// --- 2. LOGIC UPLOAD: FIX NAMA SELLER ---
fun uploadListing(
    context: android.content.Context,
    title: String,
    price: String,
    category: String,
    condition: String,
    desc: String,
    imageUri: Uri,
    onSuccess: () -> Unit,
    onFail: (String) -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser ?: return onFail("User not logged in!")

    val firestore = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference
    val fileName = "product_images/${UUID.randomUUID()}.jpg"
    val imageRef = storageRef.child(fileName) // <--- Dah sedia kat sini!

    firestore.collection("users").document(currentUser.uid).get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val studentId = document.getString("studentId") ?: "Unknown"
                val sellerFaculty = getFacultyFromId(studentId)

                val nameFromDb = document.getString("fullName") ?: document.getString("name")
                val sellerName = nameFromDb ?: currentUser.displayName ?: "Unknown User"
                val sellerEmail = document.getString("email") ?: currentUser.email ?: ""

                // PROSES UPLOAD
                imageRef.putFile(imageUri).addOnSuccessListener {
                    // FIX: BUANG KURUNGAN KAT 'uri'!
                    imageRef.downloadUrl.addOnSuccessListener { uri: android.net.Uri ->

                        val productMap = hashMapOf(
                            "title" to title,
                            "price" to price,
                            "category" to category,
                            "condition" to condition,
                            "description" to desc,
                            "imgUrl" to uri.toString(),
                            "sellerId" to studentId,
                            "sellerFaculty" to sellerFaculty,
                            "sellerAuthId" to currentUser.uid,
                            "sellerName" to sellerName,
                            "sellerEmail" to sellerEmail,
                            "status" to "pending",
                            "timestamp" to System.currentTimeMillis()
                        )

                        firestore.collection("products").add(productMap)
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { onFail(it.message ?: "Database Error") }
                    }
                }.addOnFailureListener { onFail("Image Upload Failed") }
            }
        }.addOnFailureListener { onFail("Verification Error") }
}

fun getFacultyFromId(studentId: String): String {
    // Kita ambil digit ke-2 & ke-3 (contoh: 03 dari D03...)
    if (studentId.length < 3) return "Unknown"
    val code = studentId.substring(1, 3)

    return when (code) {
        "02" -> "FKE"
        "03" -> "FTMK" //
        "04" -> "FKM"
        "05" -> "FKEKK"
        "06" -> "FKP"
        "07" -> "FPTT"
        "08" -> "FTK"
        else -> "Other"
    }
}