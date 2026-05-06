package com.utem.nougat.campuscycle

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.utem.nougat.campuscycle.navigation.AppNavigation
import com.utem.nougat.campuscycle.ui.theme.CampusBlue
import com.utem.nougat.campuscycle.ui.theme.CampusCycleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            var startScreen by remember { mutableStateOf<String?>(null) }
            var pendingRoute by remember { mutableStateOf<String?>(null) }

            val auth = FirebaseAuth.getInstance()
            val db = FirebaseFirestore.getInstance()

            // --- NAVIGATION SETUP SAHAJA (TIADA CCTV KICK DI SINI) ---
            LaunchedEffect(Unit, auth.currentUser) {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    startScreen = "login"
                } else {
                    // Update token bila buka app
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        db.collection("users").document(currentUser.uid).update("fcmToken", token)
                    }
                    // Check role untuk tentukan haluan
                    db.collection("users").document(currentUser.uid).get()
                        .addOnSuccessListener { doc ->
                            val role = doc.getString("role") ?: "user"
                            val navTo = intent.getStringExtra("nav_to")
                            if (role == "admin") startScreen = "admin_home"
                            else startScreen = if (navTo == "notification_tab") "user_home_noti" else "user_home"
                        }
                        .addOnFailureListener { startScreen = "login" }
                }
            }

            // Notification Listener
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { newIntent ->
                    if (newIntent.getStringExtra("nav_to") == "notification_tab" && auth.currentUser != null) {
                        pendingRoute = "user_home_noti"
                    }
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }

            CampusCycleTheme(darkTheme = isDarkTheme) {
                if (startScreen == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CampusBlue)
                    }
                } else {
                    key(startScreen) {
                        AppNavigation(
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                            startDestination = startScreen!!,
                            pendingRoute = pendingRoute,
                            onNavigated = { pendingRoute = null }
                        )
                    }
                }
            }
        }
    }
}