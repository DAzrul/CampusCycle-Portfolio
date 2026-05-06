package com.utem.nougat.campuscycle.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.utem.nougat.campuscycle.SessionManager
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import java.util.UUID

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    var studentId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPreferences = remember {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "SecureCampusPrefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    LaunchedEffect(Unit) {
        SessionManager.isLoggingOut = false
        SessionManager.isLoggingIn = false

        val savedId = sharedPreferences.getString("SAVED_ID", null)
        val savedPass = sharedPreferences.getString("SAVED_PASS", null)
        val savedRemember = sharedPreferences.getBoolean("IS_REMEMBERED", false)
        if (savedRemember && savedId != null && savedPass != null) {
            studentId = savedId
            password = savedPass
            isRememberMe = true
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(onDismiss = { showForgotPasswordDialog = false }, onSendClick = { email ->
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnSuccessListener {
                showForgotPasswordDialog = false; Toast.makeText(context, "Link dihantar ke $email", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show() }
        })
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(80.dp).background(CampusBlue, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ShoppingCart, "Logo", tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("CampusCycle", fontSize = 32.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(40.dp))

        OutlinedTextField(
            value = studentId, onValueChange = { studentId = it }, label = { Text("Student ID") },
            leadingIcon = { Icon(Icons.Outlined.Badge, null) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            leadingIcon = { Icon(Icons.Outlined.Lock, null) },
            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null) } },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isRememberMe = !isRememberMe }) {
                Checkbox(checked = isRememberMe, onCheckedChange = { isRememberMe = it }, colors = CheckboxDefaults.colors(checkedColor = CampusBlue))
                Text("Remember Me", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            TextButton(onClick = { showForgotPasswordDialog = true }) { Text("Forgot Password", color = CampusBlue, fontSize = 14.sp) }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (studentId.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    // 1. ANGKAT BENDERA
                    SessionManager.isLoggingIn = true

                    viewModel.loginUser(
                        inputId = studentId,
                        pass = password,
                        onSuccess = { role ->
                            val uid = FirebaseAuth.getInstance().currentUser?.uid
                            val db = FirebaseFirestore.getInstance()

                            if (uid != null) {
                                // 2. BERSIHKAN PHONE (Local) DULU
                                val editor = sharedPreferences.edit()
                                editor.remove("SESSION_ID")
                                editor.commit() // Guna commit (blocking) supaya pasti bersih

                                val newSessionId = UUID.randomUUID().toString()

                                // 3. UPDATE DATABASE (Remote)
                                FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                                    val updates = hashMapOf<String, Any>("currentSessionId" to newSessionId, "fcmToken" to token)

                                    db.collection("users").document(uid).update(updates)
                                        .addOnSuccessListener {

                                            // 4. UPDATE PHONE (Local)
                                            editor.putString("SESSION_ID", newSessionId)
                                            if (isRememberMe) {
                                                editor.putString("SAVED_ID", studentId)
                                                editor.putString("SAVED_PASS", password)
                                                editor.putBoolean("IS_REMEMBERED", true)
                                            } else {
                                                editor.remove("SAVED_ID")
                                                editor.remove("SAVED_PASS")
                                                editor.putBoolean("IS_REMEMBERED", false)
                                            }
                                            editor.commit() // Confirm save

                                            isLoading = false
                                            Toast.makeText(context, "Login Berjaya!", Toast.LENGTH_SHORT).show()

                                            // 5. TURUNKAN BENDERA
                                            SessionManager.isLoggingIn = false
                                            onLoginSuccess(role)
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            SessionManager.isLoggingIn = false
                                            FirebaseAuth.getInstance().signOut()
                                            Toast.makeText(context, "Database Fail", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        },
                        onError = { error ->
                            isLoading = false
                            SessionManager.isLoggingIn = false
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Sila masukkan ID & Password", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CampusBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("Log In", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account? ", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Sign Up", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CampusBlue, modifier = Modifier.clickable { onNavigateToRegister() })
        }
    }
}
@Composable
fun ForgotPasswordDialog(onDismiss: () -> Unit, onSendClick: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
        text = { Column { Text("Enter your email address."); Spacer(modifier = Modifier.height(16.dp)); OutlinedTextField(value = email, onValueChange = { email = it; errorMessage = null }, label = { Text("Email Address") }, leadingIcon = { Icon(Icons.Outlined.Email, null) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth()); if (errorMessage != null) Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } },
        confirmButton = { Button(onClick = { if (email.isBlank()) errorMessage = "Enter email" else { isLoading = true; onSendClick(email) } }, enabled = !isLoading, colors = ButtonDefaults.buttonColors(containerColor = CampusBlue)) { if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Send Link", color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }, containerColor = MaterialTheme.colorScheme.surface
    )
}