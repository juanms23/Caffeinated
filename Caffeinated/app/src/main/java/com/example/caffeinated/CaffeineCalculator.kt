package com.example.caffeinated

import com.example.caffeinated.models.CaffeineDrink
import com.example.caffeinated.models.CaffeinePoint
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.pow

class CaffeineCalculator {
    companion object {
        private const val CAFFEINE_HALF_LIFE_HOURS = 5.0
        private const val DECAY_RATE = -0.693 / CAFFEINE_HALF_LIFE_HOURS

        fun calculateCaffeineLevel(
            drinks: List<CaffeineDrink>,
            time: LocalDateTime,
        ): Double =
            drinks.sumOf { drink ->
                drink.timeConsumed?.let { consumed ->
                    val hoursSinceDrink =
                        (time.hour - consumed.hour) +
                            (time.minute - consumed.minute) / 60.0
                    if (hoursSinceDrink < 0) {
                        0.0
                    } else {
                        drink.caffeineMg * Math.E.pow(DECAY_RATE * hoursSinceDrink)
                    }
                } ?: 0.0
            }

        fun calculateDayChart(drinks: List<CaffeineDrink>): List<CaffeinePoint> {
            val points = mutableListOf<CaffeinePoint>()
            val startTime = LocalTime.of(6, 0)
            val endTime = LocalTime.of(22, 0)

            var currentTime = startTime
            while (currentTime <= endTime) {
                val currentDateTime = LocalDateTime.now().with(currentTime)
                val caffeineLevel = calculateCaffeineLevel(drinks, currentDateTime)
                points.add(CaffeinePoint(currentTime, caffeineLevel))
                currentTime = currentTime.plusMinutes(30)
            }

            return points
        }
    }
}
