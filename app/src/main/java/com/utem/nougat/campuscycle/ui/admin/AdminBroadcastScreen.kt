package com.utem.nougat.campuscycle.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.utem.nougat.campuscycle.ui.theme.CampusBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBroadcastScreen(
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    // Add theme detection (same as other screens)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDark) MaterialTheme.colorScheme.background else Color(0xFFF3F4F6)
    val cardColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val topBarColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
    val textColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Broadcast Notification", fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Campaign,
                contentDescription = null,
                tint = CampusBlue,
                modifier = Modifier.size(80.dp)
            )

            Text(
                "Send Message to All Users",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // INPUT TITLE
                    Text("Notification Title", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g., Happy Holidays!") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // INPUT MESSAGE
                    Text("Message Content", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        placeholder = { Text("Write your message here...") },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // SEND BUTTON
                    Button(
                        onClick = {
                            if (title.isBlank() || message.isBlank()) {
                                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSending = true
                            // Tarik semua user ID
                            db.collection("users").get().addOnSuccessListener { userDocs ->
                                val batch = db.batch()

                                for (doc in userDocs) {
                                    val newNotiRef = db.collection("notifications").document()
                                    val broadcastData = hashMapOf(
                                        "recipientId" to doc.id,
                                        "senderId" to "admin_system",
                                        "title" to title,
                                        "message" to message,
                                        "type" to "system", // Akan keluar icon megaphone
                                        "timestamp" to System.currentTimeMillis(),
                                        "read" to false
                                    )
                                    batch.set(newNotiRef, broadcastData)
                                }

                                // Submit semua sekaligus (Batch update lagi laju)
                                batch.commit().addOnSuccessListener {
                                    isSending = false
                                    title = ""
                                    message = ""
                                    Toast.makeText(context, "Broadcast sent to ${userDocs.size()} users!", Toast.LENGTH_LONG).show()
                                }.addOnFailureListener {
                                    isSending = false
                                    Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CampusBlue),
                        enabled = !isSending,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Broadcast Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}