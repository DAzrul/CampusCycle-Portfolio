package com.utem.nougat.campuscycle

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.utem.nougat.campuscycle.MainActivity // Pastikan import MainActivity betul
import com.utem.nougat.campuscycle.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Bila token baru generated (first install atau token refresh)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    // Bila notifikasi masuk
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "New Alert"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Check the app"

        showNotification(title, body)
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Simpan token ni dalam user profile supaya server tahu nak tembak siapa
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("fcmToken", token)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "campus_cycle_fcm"
        val channelName = "CampusCycle Alerts"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            // TAMBAH INI: "Flag" bagitahu nak pergi mana
            putExtra("nav_to", "notification_tab")
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Pastikan icon wujud!
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}