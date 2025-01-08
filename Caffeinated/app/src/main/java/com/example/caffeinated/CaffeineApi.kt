package com.example.caffeinated

import com.example.caffeinated.models.CaffeineDrink
import org.json.JSONArray
import java.net.URL

class CaffeineApi {
    companion object {
        private const val BASE_URL = "https://coffee-ef9cb-default-rtdb.firebaseio.com"

        // Fetch all drinks from Firebase
        fun getAllDrinks(): List<CaffeineDrink> {
            val url = URL("$BASE_URL/.json")
            val response = url.readText()
            print(response)
            val jsonArray = JSONArray(response)

            return List(jsonArray.length()) { index ->
                val item = jsonArray.getJSONObject(index)
                CaffeineDrink(
                    caffeineMg = item.getInt("Caffeine (mg)"),
                    calories = item.getInt("Calories"),
                    volumeMl = item.getString("Volume (ml)"),
                    drink = item.getString("drink"),
                    type = item.getString("type"),
                )
            }
        }

        // Search drinks by name (case-insensitive)
        fun searchDrinks(query: String): List<CaffeineDrink> {
            val allDrinks = getAllDrinks()
            return allDrinks.filter { it.drink.contains(query, ignoreCase = true) }
        }

        // Filter drinks by type
        fun filterDrinksByType(type: String): List<CaffeineDrink> {
            val allDrinks = getAllDrinks()
            return allDrinks.filter { it.type.equals(type, ignoreCase = true) }
        }

        // Search and filter drinks by both name and type
        fun searchAndFilterDrinks(
            query: String,
            type: String?,
        ): List<CaffeineDrink> {
            val allDrinks = getAllDrinks()
            return allDrinks.filter {
                it.drink.contains(query, ignoreCase = true) &&
                    (type == null || it.type.equals(type, ignoreCase = true))
            }
        }
    }
}
