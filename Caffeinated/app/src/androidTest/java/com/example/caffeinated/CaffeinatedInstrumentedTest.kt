package com.example.caffeinated

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.caffeinated.models.CaffeineDrink
import com.example.caffeinated.models.Screens
import com.example.caffeinated.ui.theme.CaffeinatedTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class CaffeinatedInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.caffeinated", appContext.packageName)
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var dbman: MyDatabaseManager
    private lateinit var navController: TestNavHostController

    @Before
    fun setUp() {
        composeTestRule.setContent {
            val context = ApplicationProvider.getApplicationContext<Context>()
            dbman = MyDatabaseManager(LocalContext.current)
            // reset for testing
            context.deleteDatabase("MyCaffeineDB")
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            CaffeinatedTheme {
                val todayEntries = listOf(
                    CaffeineDrink(100, 10, "200", "Coffee", "Coffee", LocalDateTime.now()),
                )

                val allEntries = listOf(
                    Pair(
                        LocalDate.now().toString(),
                        CaffeineDrink(100, 10, "200", "Coffee", "Coffee", LocalDateTime.now())
                    ),
                    Pair(
                        LocalDate.now().toString(),
                        CaffeineDrink(50, 5, "100", "Tea", "Tea", LocalDateTime.now())
                    )
                )

                NavHost(
                    navController = navController,
                    startDestination = Screens.HOME.name
                ) {
                    composable(Screens.HOME.name) {
                        HomeScreen(
                            todayEntries = todayEntries,
                            navigateToDrinkScreen = {
                                navController.navigate(Screens.DRINK_LIST.name)
                            },
                            navController = navController
                        )
                    }

                    composable(Screens.DRINK_LIST.name) {
                        DrinkListScreen(
                            dbman = dbman,
                            updateEntries = {},
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            navController = navController
                        )
                    }

                    composable(Screens.TODAY.name) {
                        TodayScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            navController = navController,
                            dbman = dbman,
                            updateEntries = {},
                        )
                    }

                    composable(Screens.HISTORY.name) {
                        HistoryScreen(
                            allEntries = allEntries,
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            navController = navController,
                            dbman = dbman,
                            updateEntries = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun displayHomeScreenAtStart() {
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)
    }

    @Test
    fun navigateFromHomeToDrinkScreen1() {
        // given home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)

        // when click to navigate to drink screen
        composeTestRule.onNodeWithTag("coffee-icon-button").performClick()

        // then should be at drinks screen
        assertEquals(Screens.DRINK_LIST.name, navController.currentDestination?.route)
    }

    @Test
    fun navigateFromHomeToDrinkScreen2() {
        // given home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)

        // when click to navigate to drink screen
        composeTestRule.onNodeWithTag("add-icon-button").performClick()

        // then should be at drinks screen
        assertEquals(Screens.DRINK_LIST.name, navController.currentDestination?.route)
    }

    @Test
    fun navigateFromHomeToDrinkScreen3() {
        // given home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)

        // when click on menu and log caffeine
        composeTestRule.onNodeWithTag("menu-button").performClick()
        composeTestRule.onNodeWithTag("log-caffeine-button").performClick()

        // then should be at drinks screen
        assertEquals(Screens.DRINK_LIST.name, navController.currentDestination?.route)
    }

    @Test
    fun navigateFromHomeToTodayScreen() {
        // given home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)

        // when click on menu and today
        composeTestRule.onNodeWithTag("menu-button").performClick()
        composeTestRule.onNodeWithTag("today-button").performClick()

        // then should be at today screen
        assertEquals(Screens.TODAY.name, navController.currentDestination?.route)
    }

    @Test
    fun navigateFromHomeToHistoryScreen() {
        // given home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)

        // when click on menu and history
        composeTestRule.onNodeWithTag("menu-button").performClick()
        composeTestRule.onNodeWithTag("history-button").performClick()

        // then should be at history screen
        assertEquals(Screens.HISTORY.name, navController.currentDestination?.route)
    }

    @Test
    fun drinkBackToHomeScreen() {
        // given drink screen
        composeTestRule.runOnUiThread {
            navController.navigate(Screens.DRINK_LIST.name)
        }

        // when click on back button
        composeTestRule
            .onNodeWithTag("back-arrow-button")
            .performClick()

        // then should be at home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)
    }

    @Test
    fun historyBackToHomeScreen() {
        // given history screen
        composeTestRule.runOnUiThread {
            navController.navigate(Screens.HISTORY.name)
        }

        // when click on back button
        composeTestRule
            .onNodeWithTag("back-arrow-button")
            .performClick()

        // then should be at home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)
    }

    @Test
    fun todayBackToHomeScreen() {
        // given today screen
        composeTestRule.runOnUiThread {
            navController.navigate(Screens.TODAY.name)
        }

        // when click on back button
        composeTestRule
            .onNodeWithTag("back-arrow-button")
            .performClick()

        // then should be at home screen
        assertEquals(Screens.HOME.name, navController.currentDestination?.route)
    }

    @Test
    fun addedDrinkOnTodayScreen() {
        // given today screen
        composeTestRule.runOnUiThread {
            navController.navigate(Screens.TODAY.name)
        }

        // when drink added today
        // then should appear on today
        composeTestRule.onNodeWithText("Coffee").assertExists()
        composeTestRule.onNodeWithText("Tea").assertDoesNotExist()
    }

    @Test
    fun addedDrinkOnHistoryScreen() {
        // given today screen
        composeTestRule.runOnUiThread {
            navController.navigate(Screens.HISTORY.name)
        }

        // when drink added today
        // then should appear on today
        composeTestRule.onNodeWithText("Coffee").assertExists()
        composeTestRule.onNodeWithText("Tea").assertExists()
    }

}
