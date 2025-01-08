package com.example.caffeinated

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class CaffeineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val notificationChannel =
            NotificationChannel(
                "caffeine_notification",
                "Caffeinated",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for caffeine tracking and warnings"
                enableLights(true)
                enableVibration(true)
            }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }
}
