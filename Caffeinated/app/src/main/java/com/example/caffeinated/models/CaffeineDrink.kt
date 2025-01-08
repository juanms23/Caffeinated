package com.example.caffeinated.models

import com.example.caffeinated.R
import java.time.LocalDateTime

data class CaffeineDrink(
    val caffeineMg: Int, // Caffeine in mg
    val calories: Int, // Calories in the drink
    val volumeMl: String, // Volume in ml
    val drink: String, // Name of the drink
    val type: String, // Type of the drink (e.g., Coffee, Tea)
    val timeConsumed: LocalDateTime? = null, // Optional timestamp for consumed drinks
) {
    val icon: Int
        get() =
            when (type.lowercase()) {
                "coffee", "espresso" -> R.drawable.ic_latte
                "tea" -> R.drawable.ic_tea
                "energy drinks", "energy shots", "soft drinks" -> R.drawable.ic_soda
                else -> R.drawable.ic_togo
            }

    // Helper function to convert this drink to a consumed drink entry
    fun toConsumedDrink(timestamp: LocalDateTime = LocalDateTime.now()): CaffeineDrink =
        this.copy(
            timeConsumed = timestamp,
        )
}