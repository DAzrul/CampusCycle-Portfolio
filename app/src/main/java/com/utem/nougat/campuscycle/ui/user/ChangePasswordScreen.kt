package com.utem.nougat.campuscycle.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(onNavigateBack: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // --- LOGIC DARK MODE ---
    // Kita detect kalau background gelap
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Warna Background Skrin
    // Light: Kelabu Cair (F3F4F6) | Dark: Ikut Tema (Hitam/Kelabu Gelap)
    val screenBackgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)

    // Warna Container TextField (Kotak Input)
    // Light: Putih | Dark: Surface (Hitam/Kelabu Gelap)
    val inputContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White

    // Warna Teks dalam Input
    val inputTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black

    // Warna TopBar
    val topBarColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val topBarContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Change Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = topBarColor,
                    titleContentColor = topBarContentColor,
                    navigationIconContentColor = topBarContentColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundColor) // <--- Dinamik Background
                .padding(padding)
                .padding(24.dp)
        ) {
            // Field: Current Password
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputContainerColor,
                    unfocusedContainerColor = inputContainerColor,
                    disabledContainerColor = inputContainerColor,
                    focusedTextColor = inputTextColor,
                    unfocusedTextColor = inputTextColor,
                    cursorColor = CampusBlue
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Field: New Password
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputContainerColor,
                    unfocusedContainerColor = inputContainerColor,
                    disabledContainerColor = inputContainerColor,
                    focusedTextColor = inputTextColor,
                    unfocusedTextColor = inputTextColor,
                    cursorColor = CampusBlue
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Field: Confirm Password
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = inputContainerColor,
                    unfocusedContainerColor = inputContainerColor,
                    disabledContainerColor = inputContainerColor,
                    focusedTextColor = inputTextColor,
                    unfocusedTextColor = inputTextColor,
                    cursorColor = CampusBlue
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Button Update
            Button(
                onClick = {
                    if (currentPassword.isEmpty() || newPassword.isEmpty()) {
                        Toast.makeText(context, "Fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        Toast.makeText(context, "New passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (user != null && user.email != null) {
                        isLoading = true
                        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
                        user.reauthenticate(credential)
                            .addOnSuccessListener {
                                user.updatePassword(newPassword)
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Password Changed!", Toast.LENGTH_LONG).show()
                                        onNavigateBack()
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        Toast.makeText(context, "Update Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Current password wrong!", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CampusBlue,
                    contentColor = Color.White // Teks butang kekal putih
                )
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White) else Text("Update Password")
            }
        }
    }
}