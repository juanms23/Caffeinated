package com.example.caffeinated

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.caffeinated.models.CaffeineDrink
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CaffeineNotificationService(
    private val context: Context,
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    private companion object {
        const val LIMIT_WARNING_ID = 1001
        const val CRASH_WARNING_ID = 1002
    }

    // warn user when approaching daily limit
    fun showLimitWarning(
        currentLevel: Int,
        dailyLimit: Int,
    ) {
        val percentageOfLimit = (currentLevel.toFloat() / dailyLimit) * 100

        if (percentageOfLimit >= 80) {
            val remainingMg = dailyLimit - currentLevel
            val notification =
                NotificationCompat
                    .Builder(context, "caffeine_notification")
                    .setContentTitle("Approaching Caffeine Limit")
                    .setContentText("You're at ${percentageOfLimit.toInt()}% of your daily limit ($remainingMg mg remaining)")
                    .setSmallIcon(R.drawable.ic_latte)
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(LIMIT_WARNING_ID, notification)
        }
    }

    // predict and warn about potential caffeine crash
    fun predictCaffeineCrash(drinks: List<CaffeineDrink>) {
        val latestDrink = drinks.maxByOrNull { it.timeConsumed ?: LocalDateTime.MIN } ?: return
        val timeConsumed = latestDrink.timeConsumed ?: return
        val crashTime = timeConsumed.plusHours(6)
        val now = LocalDateTime.now()

        if (now.until(crashTime, ChronoUnit.MINUTES) in 0..30) {
            val notification =
                NotificationCompat
                    .Builder(context, "caffeine_notification")
                    .setContentTitle("Caffeine Crash Warning")
                    .setContentText("You might experience a caffeine crash soon. Consider taking a break or having a light snack.")
                    .setSmallIcon(R.drawable.ic_latte)
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setAutoCancel(true)
                    .setStyle(
                        NotificationCompat
                            .BigTextStyle()
                            .bigText(
                                "You might experience a caffeine crash soon. Consider:\n" +
                                    "• Taking a short break\n" +
                                    "• Having a light snack\n" +
                                    "• Drinking water\n" +
                                    "• Getting some fresh air",
                            ),
                    ).build()

            notificationManager.notify(CRASH_WARNING_ID, notification)
        }
    }
}
