package com.utem.nougat.campuscycle.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utem.nougat.campuscycle.ui.admin.AdminBroadcastScreen
import com.utem.nougat.campuscycle.ui.auth.WelcomeScreen
import com.utem.nougat.campuscycle.ui.auth.LoginScreen
import com.utem.nougat.campuscycle.ui.auth.RegisterScreen
import com.utem.nougat.campuscycle.ui.user.UserHomeScreen
import com.utem.nougat.campuscycle.ui.admin.AdminDashboardScreen
import com.utem.nougat.campuscycle.ui.user.EditProfileScreen
import com.utem.nougat.campuscycle.ui.user.SettingsScreen
import com.utem.nougat.campuscycle.ui.user.ChangePasswordScreen
import com.utem.nougat.campuscycle.ui.user.UpdateListingScreen
import com.utem.nougat.campuscycle.ui.user.DetailItemScreen
import com.utem.nougat.campuscycle.ui.user.FavoritesScreen
import com.utem.nougat.campuscycle.ui.user.TransactionHistoryScreen

@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    startDestination: String,
    pendingRoute: String? = null,
    onNavigated: () -> Unit = {}
) {
    val navController = rememberNavController()

    // --- LOGIC TURBO: HANDLE NOTIFICATION CLICK ---
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            // Kalau user tekan noti, kita reset stack dan bawa ke Home Tab Noti
            navController.navigate(pendingRoute) {
                popUpTo(0) { inclusive = true } // Cuci bersih sebelum masuk
            }
            onNavigated()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ){

        // 1. WELCOME SCREEN
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        // 2. LOGIN SCREEN
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    // 🔥 UPDATE: Guna popUpTo(0) supaya user tak boleh back ke Login
                    if (role == "admin") {
                        navController.navigate("admin_home") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate("user_home") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        // 3. REGISTER SCREEN
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 4. USER HOME SCREEN (DEFAULT)
        composable("user_home") {
            UserHomeScreen(
                isDarkTheme = isDarkTheme,
                initialTab = 0,
                onNavigateToDetail = { route -> navController.navigate(route) },
                onLogout = {
                    // 🔥 UPDATE: Cara Logout paling bersih (Cuci semua sejarah screen)
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onEditProfile = { navController.navigate("edit_profile") },
                onSettingsClick = { navController.navigate("settings") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }

        // 4.5. USER HOME KHAS (DARI NOTIFIKASI)
        composable("user_home_noti") {
            UserHomeScreen(
                isDarkTheme = isDarkTheme,
                initialTab = 1,
                onNavigateToDetail = { route -> navController.navigate(route) },
                onLogout = {
                    // 🔥 UPDATE: Samakan dengan atas (Cuci bersih)
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onEditProfile = { navController.navigate("edit_profile") },
                onSettingsClick = { navController.navigate("settings") },
                onNavigateToFavorites = { navController.navigate("favorites") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }

        // 5. ADMIN DASHBOARD SCREEN
        composable("admin_home") {
            AdminDashboardScreen(
                onLogout = {
                    // 🔥 UPDATE: Samakan dengan atas (Cuci bersih)
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onEditProfile = { navController.navigate("edit_profile") },
                onSettingsClick = { navController.navigate("settings") },
                onNavigateToBroadcast = { navController.navigate("admin_broadcast") }
            )
        }

        // 5.1. ADMIN BROADCAST
        composable("admin_broadcast") {
            AdminBroadcastScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. EDIT PROFILE SCREEN
        composable("edit_profile") {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7. SETTINGS SCREEN
        composable("settings") {
            SettingsScreen(
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate("change_password") }
            )
        }

        // 8. CHANGE PASSWORD SCREEN
        composable("change_password") {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 9. UPDATE LIST SCREEN
        composable("update_listing/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            UpdateListingScreen(
                isDarkTheme = isDarkTheme,
                productId = productId,
                onNavigateBack = { navController.popBackStack() },
                onUpdateSuccess = { navController.popBackStack() }
            )
        }

        // 10. PRODUCT DETAIL SCREEN
        composable("product_detail/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: return@composable
            DetailItemScreen(
                isDarkTheme = isDarkTheme,
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 11. FAVORITES SCREEN
        composable("favorites") {
            FavoritesScreen(
                isDarkTheme = isDarkTheme,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { route -> navController.navigate(route) }
            )
        }

        // 12. TRANSACTION HISTORY SCREEN
        composable("history") {
            TransactionHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}