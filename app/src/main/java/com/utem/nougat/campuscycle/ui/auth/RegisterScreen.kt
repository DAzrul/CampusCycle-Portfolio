package com.utem.nougat.campuscycle.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import com.utem.nougat.campuscycle.ui.theme.CampusCycleTheme

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // --- STATE INPUT ---
    var fullName by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // --- STATE UI ---
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // --- WARNA DINAMIK (Dark Mode logic) ---
    val inputContainerColor = MaterialTheme.colorScheme.surface
    val inputContentColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Background ikut tema
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .imePadding() // Keyboard fix
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // --- LOGO KOTAK BIRU ---
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(CampusBlue, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tajuk
        Text(
            text = "CampusCycle",
            fontSize = 32.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Subtajuk
        Text(
            text = "Create your account",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- INPUT FIELDS ---

        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            leadingIcon = { Icon(Icons.Outlined.Person, null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputContainerColor,
                unfocusedContainerColor = inputContainerColor,
                focusedTextColor = inputContentColor,
                unfocusedTextColor = inputContentColor
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Student ID
        OutlinedTextField(
            value = studentId,
            // 🔥 UBAH BARIS INI 🔥
            // Ini akan tukar input jadi HURUF BESAR serta-merta masa user taip.
            // Contoh: User tekan 'd', skrin keluar 'D'.
            onValueChange = { studentId = it.uppercase() },

            label = { Text("Student ID") },
            leadingIcon = { Icon(Icons.Outlined.Badge, null) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputContainerColor,
                unfocusedContainerColor = inputContainerColor,
                focusedTextColor = inputContentColor,
                unfocusedTextColor = inputContentColor
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            leadingIcon = { Icon(Icons.Outlined.Email, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputContainerColor,
                unfocusedContainerColor = inputContainerColor,
                focusedTextColor = inputContentColor,
                unfocusedTextColor = inputContentColor
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputContainerColor,
                unfocusedContainerColor = inputContainerColor,
                focusedTextColor = inputContentColor,
                unfocusedTextColor = inputContentColor
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null)
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inputContainerColor,
                unfocusedContainerColor = inputContainerColor,
                focusedTextColor = inputContentColor,
                unfocusedTextColor = inputContentColor
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // --- BUTTONS ---

        // SIGN UP BUTTON
        Button(
            onClick = {
                if (fullName.isBlank() || studentId.isBlank() || email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                } else if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                } else {
                    isLoading = true
                    viewModel.registerStudent(
                        fullName = fullName,
                        studentId = studentId.uppercase(), // <--- Tambah .uppercase() kat sini juga
                        email = email,
                        pass = password,
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Account Created! Please Log In.", Toast.LENGTH_LONG).show()
                            onRegisterSuccess()
                        },
                        onError = { error ->
                            isLoading = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CampusBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Sign Up", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TEKS CLICKABLE (Ganti Button Kelabu Dulu) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Log In",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CampusBlue, // Warna Biru supaya nampak macam link
                modifier = Modifier.clickable { onNavigateToLogin() } // <--- Tekan sini pergi Login
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    CampusCycleTheme {
        RegisterScreen(onRegisterSuccess = {}, onNavigateToLogin = {})
    }
}