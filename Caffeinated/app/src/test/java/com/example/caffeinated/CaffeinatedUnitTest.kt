package com.example.caffeinated

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

class CaffeineApiUnitTest {
    companion object {
        private const val MIN_DRINKS_SIZE = 10
    }

    @Test
    fun testGetAllDrinks() {
        val drinks = CaffeineApi.getAllDrinks()
        assert(drinks.size >= MIN_DRINKS_SIZE)
    }

    @Test
    fun testSearchDrinks() {
        val query = "Costa Coffee"
        val drinks = CaffeineApi.searchDrinks(query)

        assert(drinks.isNotEmpty())
        assert(drinks.any { it.drink.contains(query, ignoreCase = true) })
    }

    @Test
    fun testFilterDrinksByType() {
        val type = "Coffee"
        val drinks = CaffeineApi.filterDrinksByType(type)

        assert(drinks.isNotEmpty())
        assert(drinks.all { it.type.equals(type, ignoreCase = true) })
    }

    @Test
    fun testSearchAndFilterDrinks() {
        val query = "Espresso"
        val type = "Coffee"
        val drinks = CaffeineApi.searchAndFilterDrinks(query, type)

        assert(drinks.isNotEmpty())
        assert(
            drinks.all {
                it.drink.contains(query, ignoreCase = true) && it.type.equals(type, ignoreCase = true)
            },
        )
    }

    @Test
    fun testInvalidDrinkSearch() {
        val query = "NonExistentDrink"
        val drinks = CaffeineApi.searchDrinks(query)

        assert(drinks.isEmpty())
    }
}
