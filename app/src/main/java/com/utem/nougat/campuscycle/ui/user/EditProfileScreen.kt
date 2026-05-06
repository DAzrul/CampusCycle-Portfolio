package com.utem.nougat.campuscycle.ui.user

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.auth.EditProfileViewModel
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    // --- STATE BORANG ---
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var businessLink by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var qrCodeUrl by remember { mutableStateOf("") }

    // --- STATE UI TAMBAHAN ---
    var isQrFitMode by remember { mutableStateOf(true) } // TRUE = Fit, FALSE = Crop

    // --- STATE LOADING ---
    var isLoadingData by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var isUploadingQR by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // --- LOGIC WARNA DARK MODE ---
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.background
    val contentColor = MaterialTheme.colorScheme.onBackground
    val topBarColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val disabledFieldColor = if (isDark) Color.DarkGray else Color(0xFFF0F0F0)

    // --- LAUNCHER GALERI (PROFILE PHOTO) ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingImage = true
            viewModel.uploadImage(uri, "profile_images",
                onSuccess = { newUrl ->
                    photoUrl = newUrl
                    isUploadingImage = false
                    Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    isUploadingImage = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    // --- LAUNCHER GALERI (QR CODE) ---
    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingQR = true
            viewModel.uploadImage(uri, "qr_codes",
                onSuccess = { newUrl ->
                    qrCodeUrl = newUrl
                    isUploadingQR = false
                    Toast.makeText(context, "QR Code uploaded!", Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    isUploadingQR = false
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        fullName = doc.getString("fullName") ?: ""
                        studentId = doc.getString("studentId") ?: ""
                        email = doc.getString("email") ?: ""
                        phoneNumber = doc.getString("phoneNumber") ?: ""
                        businessLink = doc.getString("businessLink") ?: ""
                        photoUrl = doc.getString("photoUrl") ?: ""
                        qrCodeUrl = doc.getString("qrCodeUrl") ?: ""
                    }
                    isLoadingData = false
                }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Update Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor
                )
            )
        }
    ) { padding ->
        if (isLoadingData) {
            Box(Modifier.fillMaxSize().background(backgroundColor), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CampusBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // --- 1. PROFILE PHOTO ---
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color.Gray else Color.LightGray)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(color = CampusBlue, modifier = Modifier.size(30.dp))
                        } else if (photoUrl.isNotEmpty()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = if (fullName.isNotEmpty()) fullName.take(1).uppercase() else "A",
                                fontSize = 40.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CampusBlue)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Text("Tap to change photo", fontSize = 12.sp, color = if (isDark) Color.LightGray else Color.Gray, modifier = Modifier.padding(top = 8.dp))

                Spacer(modifier = Modifier.height(32.dp))

                // --- 2. BORANG ---
                EditProfileField("Full Name *", fullName, { fullName = it }, isDark = isDark)
                EditProfileField("Student/Staff ID *", studentId, readOnly = true, helperText = "Cannot be changed", isDark = isDark, disabledColor = disabledFieldColor)
                EditProfileField("Email Address *", email, readOnly = true, helperText = "Cannot be changed", isDark = isDark, disabledColor = disabledFieldColor)
                EditProfileField("Phone Number *", phoneNumber, { phoneNumber = it }, placeholder = "+60 12 3456 789", isDark = isDark)
                EditProfileField("Business Link (WhatsApp)", businessLink, { businessLink = it }, placeholder = "https://wa.link/...", isDark = isDark)

                // --- 3. QR CODE UPLOAD & ADJUST ---

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "WhatsApp QR Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color.Black
                    )

                    // Butang Toggle Mode (Fit / Crop)
                    if (qrCodeUrl.isNotEmpty()) {
                        TextButton(onClick = { isQrFitMode = !isQrFitMode }) {
                            Icon(
                                if (isQrFitMode) Icons.Default.Crop else Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isQrFitMode) "Mode: Fit" else "Mode: Fill", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // Besar sikit untuk preview
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF2C2C2C) else Color(0xFFF5F6F8))
                        .border(1.dp, if (isDark) Color.Gray else Color.LightGray, RoundedCornerShape(12.dp))
                        // Kalau klik gambar, dia toggle mode jugak
                        .clickable {
                            if(qrCodeUrl.isEmpty()) qrPickerLauncher.launch("image/*")
                            else isQrFitMode = !isQrFitMode
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingQR) {
                        CircularProgressIndicator(color = CampusBlue)
                    } else if (qrCodeUrl.isNotEmpty()) {
                        // Gambar QR
                        AsyncImage(
                            model = qrCodeUrl,
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize(),
                            // INI LOGIC DIA:
                            contentScale = if (isQrFitMode) ContentScale.Fit else ContentScale.Crop
                        )

                        // Action Buttons Overlay
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        ) {
                            // Butang Upload Baru
                            IconButton(
                                onClick = { qrPickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Black.copy(0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Add, "Change QR", tint = Color.White, modifier = Modifier.size(16.dp))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Butang Delete
                            IconButton(
                                onClick = { qrCodeUrl = "" },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.Red.copy(0.7f), CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, "Remove QR", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Hint text kat bawah
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(0.4f))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Tap image to adjust view",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }

                    } else {
                        // Kalau kosong
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCode, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tap to upload QR Image", color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- 4. BUTTONS ---
                Button(
                    onClick = {
                        isSaving = true
                        viewModel.updateProfile(
                            newName = fullName,
                            newPhone = phoneNumber,
                            newLink = businessLink,
                            newQrUrl = qrCodeUrl,
                            onSuccess = {
                                isSaving = false
                                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            onError = { error ->
                                isSaving = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CampusBlue)
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE0E0E0),
                        contentColor = contentColor
                    )
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

// --- KOMPONEN CUSTOM ---
@Composable
fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    readOnly: Boolean = false,
    helperText: String? = null,
    placeholder: String = "",
    isDark: Boolean,
    disabledColor: Color = Color(0xFFF0F0F0)
) {
    val labelColor = if (isDark) Color.White else Color.Black
    val containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDark) Color.White else Color.Black
    val placeholderColor = if (isDark) Color.LightGray else Color.Gray
    val borderColor = if (isDark) Color.Gray else Color.LightGray

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = labelColor)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            readOnly = readOnly,
            enabled = !readOnly,
            placeholder = { Text(placeholder, color = placeholderColor) },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = disabledColor,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                focusedBorderColor = CampusBlue,
                unfocusedBorderColor = borderColor,
                disabledBorderColor = borderColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                disabledTextColor = if (isDark) Color.LightGray else Color.Gray
            ),
            singleLine = true
        )
        if (helperText != null) {
            Text(helperText, fontSize = 12.sp, color = placeholderColor, modifier = Modifier.padding(top = 4.dp))
        }
    }
}