package com.example.caffeinated

import androidx.compose.runtime.*
import com.example.caffeinated.models.CaffeineDrink
import java.time.LocalDate
import java.time.LocalDateTime

object CaffeineTracker {
    private val dailyLimit = mutableIntStateOf(200)
    private var notificationService: CaffeineNotificationService? = null
    private const val LIMIT_WARNING_THRESHOLD = 0.8

    fun initializeNotifications(service: CaffeineNotificationService) {
        notificationService = service
    }

    private fun getCurrentCaffeineLevel(dbManager: MyDatabaseManager): Int {
        val now = LocalDateTime.now()
        val todayDrinks = dbManager.readToday(LocalDate.now().toString())
        return CaffeineCalculator.calculateCaffeineLevel(todayDrinks, now).toInt()
    }

    fun triggerNotificationChecks(dbManager: MyDatabaseManager) {
        notificationService?.let { service ->
            val currentLevel = getCurrentCaffeineLevel(dbManager)
            val dailyLimit = getDailyLimit()

            if (currentLevel >= dailyLimit * LIMIT_WARNING_THRESHOLD) {
                service.showLimitWarning(currentLevel, dailyLimit)
            }

            service.predictCaffeineCrash(getDrinks(dbManager))
        }
    }

    private fun getDrinks(dbManager: MyDatabaseManager): List<CaffeineDrink> = dbManager.readToday(LocalDate.now().toString())

    fun getDailyLimit(): Int = dailyLimit.intValue
}
