package com.utem.nougat.campuscycle.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.utem.nougat.campuscycle.MainActivity
import com.utem.nougat.campuscycle.R

object NotificationHelper {

    private const val CHANNEL_ID = "campus_cycle_channel"
    private const val CHANNEL_NAME = "CampusCycle Notifications"

    fun showNotification(context: Context, title: String, message: String) {

        // 1. Setup Channel (Wajib untuk Android 8.0 ke atas)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Bunyi kuat & Popup
            ).apply {
                description = "Notifikasi penting dari CampusCycle"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // 2. Intent: Apa jadi bila user tekan noti tu?
        val intent = Intent(context, MainActivity::class.java).apply {
            // JANGAN GUNA CLEAR_TASK! Guna SINGLE_TOP supaya dia tak kill app.
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Design rupa notifikasi
        // NOTE: Pastikan icon (R.drawable.ic_launcher_foreground) wujud, kalau tak crash!
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Hilang lepas tekan
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // 4. Tembak Notifikasi (Check Permission dulu)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}