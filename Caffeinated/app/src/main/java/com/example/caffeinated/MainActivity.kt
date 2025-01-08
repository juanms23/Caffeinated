package com.example.caffeinated

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.caffeinated.models.CaffeineDrink
import com.example.caffeinated.models.CaffeinePoint
import com.example.caffeinated.models.Screens
import com.example.caffeinated.ui.theme.CaffeinatedTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// need to ask user for what their caffeine max is
class MainActivity : ComponentActivity() {
    private lateinit var notificationService: CaffeineNotificationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val dbman = MyDatabaseManager(this)

        notificationService = CaffeineNotificationService(this)
        CaffeineTracker.initializeNotifications(notificationService)

        setContent {
            CaffeinatedTheme {
                var showPermissionDialog by remember { mutableStateOf(false) }
                var showSettingsDialog by remember { mutableStateOf(false) }

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission(),
                    ) { isGranted ->
                        if (isGranted) {
                            notificationService = CaffeineNotificationService(this)
                            CaffeineTracker.initializeNotifications(notificationService)
                        } else {
                            showSettingsDialog = true
                        }
                    }

                LaunchedEffect(Unit) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    when {
                        notificationManager.areNotificationsEnabled() -> {}
                        shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                            showPermissionDialog = true
                        }

                        else -> {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                if (showPermissionDialog) {
                    AlertDialog(
                        onDismissRequest = { showPermissionDialog = false },
                        title = { Text("Notification Permission") },
                        text = {
                            Text(
                                "We need notification permission to remind you about logging your caffeine intake and warn you about reaching your daily limit. Would you like to enable notifications?",
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showPermissionDialog = false
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                },
                            ) {
                                Text("Enable")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPermissionDialog = false }) {
                                Text("Not Now")
                            }
                        },
                    )
                }

                if (showSettingsDialog) {
                    AlertDialog(
                        onDismissRequest = { showSettingsDialog = false },
                        title = { Text("Permission Required") },
                        text = { Text("Notifications are important for tracking your caffeine intake. Please enable them in Settings.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showSettingsDialog = false
                                    startActivity(
                                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                        },
                                    )
                                },
                            ) {
                                Text("Open Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showSettingsDialog = false }) {
                                Text("Not Now")
                            }
                        },
                    )
                }

                val today = remember { LocalDate.now() }
                val scope = rememberCoroutineScope()
                var allEntries by remember { mutableStateOf<List<Pair<String, CaffeineDrink>>>(emptyList()) }
                var todayEntries by remember { mutableStateOf<List<CaffeineDrink>>(emptyList()) }

                val updateAllEntries = {
                    scope.launch(Dispatchers.IO) {
                        val entries = dbman.readEntries()
                        withContext(Dispatchers.Main) {
                            allEntries = entries
                        }
                    }
                }

                val updateTodayEntries = {
                    scope.launch(Dispatchers.IO) {
                        val entries = dbman.readToday(today.toString())
                        withContext(Dispatchers.Main) {
                            todayEntries = entries
                        }
                    }
                }

                // load from database
                LaunchedEffect(true) {
                    withContext(Dispatchers.IO) {
                        val cachedEntries = dbman.readEntries()
                        val cachedTodayEntries = dbman.readToday(today.toString())
                        allEntries = cachedEntries
                        todayEntries = cachedTodayEntries
                    }
                }

                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = Screens.HOME.name,
                ) {
                    composable(Screens.HOME.name) {
                        HomeScreen(
                            todayEntries = todayEntries,
                            navigateToDrinkScreen = {
                                navController.navigate(Screens.DRINK_LIST.name)
                            },
                            navController = navController,
                        )
                    }

                    composable(Screens.DRINK_LIST.name) {
                        DrinkListScreen(
                            dbman = dbman,
                            updateEntries = {
                                updateAllEntries()
                                updateTodayEntries()
                            },
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            navController = navController,
                        )
                    }

                    composable(Screens.TODAY.name) {
                        TodayScreen(
                            onNavigateBack = {
                                navController.popBackStack()
                            },
                            navController = navController,
                            dbman = dbman,
                            updateEntries = {
                                updateAllEntries()
                                updateTodayEntries()
                            },
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
                            updateEntries = {
                                updateAllEntries()
                                updateTodayEntries()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    navigateToDrinkScreen: () -> Unit = {},
    navController: NavController,
    todayEntries: List<CaffeineDrink>,
) {
    Scaffold(
        modifier = Modifier,
        topBar = {
            CaffeinatedTopBar(
                onNavigateToScreen = { screen ->
                    navController.navigate(screen)
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(paddingValues)
                    .padding(16.dp),
            horizontalAlignment = CenterHorizontally,
        ) {
            val todaySum = remember(todayEntries) { todayEntries.sumOf { it.caffeineMg } }

            CaffeineProgressIndicator(
                currentAmount = todaySum,
                maxAmount = CaffeineTracker.getDailyLimit(),
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = navigateToDrinkScreen,
                modifier =
                    Modifier
                        .size(300.dp)
                        .testTag("coffee-icon-button"),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.coffee),
                    contentDescription = "coffee Image",
                    modifier = Modifier.size(275.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = navigateToDrinkScreen,
                modifier =
                    Modifier
                        .size(64.dp)
                        .background(
                            color = Color.Black,
                            shape = CircleShape,
                        ).padding(16.dp)
                        .testTag("add-icon-button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Drink",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CaffeineProgressIndicator(
    currentAmount: Int,
    maxAmount: Int,
) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = CenterVertically,
        ) {
            Text(
                text = "$currentAmount mg",
                color = if (currentAmount > maxAmount) Color(0xFFD83843) else Color.White,
                fontSize = 20.sp,
            )
            Text(
                text = "$maxAmount mg",
                color = Color.White,
                fontSize = 20.sp,
            )
        }

        LinearProgressIndicator(
            progress = { currentAmount.toFloat() / maxAmount },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
            color = Color(101, 67, 33),
            trackColor = Color.White,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaffeinatedTopBar(
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onNavigateToScreen: (String) -> Unit = {},
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        colors =
            topAppBarColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                titleContentColor = Color.White,
            ),
        title = {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.clickable { onNavigateToScreen(Screens.HOME.name) },
            )
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick,
                    modifier = Modifier.testTag("back-arrow-button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = { expanded = !expanded },
                modifier = Modifier.testTag("menu-button")) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Log Caffeine", color = MaterialTheme.colorScheme.onSecondary) },
                    onClick = {
                        expanded = false
                        onNavigateToScreen(Screens.DRINK_LIST.name)
                    },
                    modifier = Modifier.testTag("log-caffeine-button")
                )
                DropdownMenuItem(
                    text = { Text("Today", color = MaterialTheme.colorScheme.onSecondary) },
                    onClick = {
                        expanded = false
                        onNavigateToScreen(Screens.TODAY.name)
                    },
                    modifier = Modifier.testTag("today-button")
                )
                DropdownMenuItem(
                    text = { Text("History", color = MaterialTheme.colorScheme.onSecondary) },
                    onClick = {
                        expanded = false
                        onNavigateToScreen(Screens.HISTORY.name)
                    },
                    modifier = Modifier.testTag("history-button")
                )
            }
        },
    )
}

private fun filterDrinks(
    allDrinks: List<CaffeineDrink>,
    query: String,
    tabIndex: Int,
    tabs: List<String>,
    recentDrinks: List<String>,
): List<CaffeineDrink> =
    allDrinks.filter { drink ->
        val matchesSearch = drink.drink.contains(query, ignoreCase = true)
        val matchesTab =
            when (tabIndex) {
                0 -> recentDrinks.contains(drink.drink)
                1 -> true
                else -> drink.type.equals(tabs[tabIndex], ignoreCase = true)
            }
        matchesSearch && matchesTab
    }

@Composable
fun DrinkItem(
    drink: CaffeineDrink,
    showTimestamp: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
            verticalAlignment = CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = drink.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    text = drink.drink,
                    fontSize = 16.sp,
                    fontWeight = if (showTimestamp) FontWeight.Medium else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onTertiary,
                    maxLines = 2,
                    lineHeight = 20.sp,
                )
                (if (!showTimestamp) drink.type else null)?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                if (showTimestamp && drink.timeConsumed != null) {
                    Text(
                        text = drink.timeConsumed.format(DateTimeFormatter.ofPattern("MM/dd/yy, h:mm a")),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${drink.caffeineMg} mg",
                fontSize = 16.sp,
                fontWeight = if (showTimestamp) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onTertiary,
            )
            Text(
                "${String.format(Locale.US, "%.1f", drink.volumeMl.toDouble() * 0.033814)} fl oz",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
fun DrinkListScreen(
    dbman: MyDatabaseManager,
    onNavigateBack: () -> Unit = {},
    navController: NavController,
    updateEntries: () -> Unit,
) {
    var allDrinks by remember { mutableStateOf<List<CaffeineDrink>>(emptyList()) }
    var filteredDrinks by remember { mutableStateOf<List<CaffeineDrink>>(emptyList()) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var recentDrinks by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf<CaffeineDrink?>(null) }
    val date = remember { LocalDate.now() }
    val time = remember { LocalDateTime.now() }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Recent", "All", "Coffee", "Tea", "Energy Drinks", "Energy Shots", "Soft Drinks")

    val updateRecentDrinks = {
        scope.launch(Dispatchers.IO) {
            val recent = dbman.getRecentDrinks()
            withContext(Dispatchers.Main) {
                recentDrinks = recent
                filteredDrinks = filterDrinks(allDrinks, searchQuery, tabIndex, tabs, recent)
            }
        }
    }

    LaunchedEffect(true) {
        withContext(Dispatchers.IO) {
            allDrinks = CaffeineApi.getAllDrinks()
            val recent = dbman.getRecentDrinks()
            withContext(Dispatchers.Main) {
                recentDrinks = recent
                filteredDrinks = filterDrinks(allDrinks, searchQuery, tabIndex, tabs, recent)
            }
        }
    }

    Scaffold(
        topBar = {
            CaffeinatedTopBar(
                showBackButton = true,
                onBackClick = onNavigateBack,
                onNavigateToScreen = { screen ->
                    navController.navigate(screen)
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = CenterVertically,
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        filteredDrinks = filterDrinks(allDrinks, query, tabIndex, tabs, recentDrinks)
                    },
                    modifier = Modifier.weight(2f),
                    placeholder = { Text("Search drinks...") },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onTertiary,
                        unfocusedTextColor = MaterialTheme.colorScheme.onTertiary,)
                )
            }

            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                containerColor = MaterialTheme.colorScheme.tertiary,
                edgePadding = 0.dp,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title, color = MaterialTheme.colorScheme.onTertiary) },
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                            filteredDrinks = filterDrinks(allDrinks, searchQuery, index, tabs, recentDrinks)
                        },
                        selectedContentColor = MaterialTheme.colorScheme.secondary,
                        unselectedContentColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredDrinks) { drink ->
                    DrinkItem(
                        drink = drink,
                        modifier =
                            Modifier.clickable {
                                showAddDialog = drink
                            },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (showAddDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = null },
            title = { Text("Add Drink", color = MaterialTheme.colorScheme.onSecondary) },
            text = { Text("Would you like to add ${showAddDialog?.drink} (${showAddDialog?.caffeineMg} mg)?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val entry = showAddDialog
                        if (entry != null) {
                            val consumedDrink = entry.toConsumedDrink()

                            dbman.insertEntry(date.toString(), time.toString(), consumedDrink)
                            updateEntries()
                            updateRecentDrinks()

                            CaffeineTracker.triggerNotificationChecks(dbman)
                        }
                        showAddDialog = null
                    },
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = null },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit = {},
    navController: NavController,
    allEntries: List<Pair<String, CaffeineDrink>>,
    dbman: MyDatabaseManager,
    updateEntries: () -> Unit,
) {
    val groupedEntries = allEntries.groupBy { it.first }
    var showRemoveDialog by remember { mutableStateOf<CaffeineDrink?>(null) }

    Scaffold(
        topBar = {
            CaffeinatedTopBar(
                showBackButton = true,
                onBackClick = onNavigateBack,
                onNavigateToScreen = { screen ->
                    navController.navigate(screen)
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                groupedEntries.forEach { (date, entries) ->
                    item {
                        val dailyTotal = entries.sumOf { it.second.caffeineMg }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.tertiary)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text =
                                    LocalDate.parse(date).format(
                                        DateTimeFormatter.ofPattern("MMMM dd yyyy"),
                                    ),
                                color = MaterialTheme.colorScheme.onTertiary,
                            )

                            Text(
                                text = "$dailyTotal mg",
                                color = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }

                    if (entries.isEmpty()) {
                        item {
                            Text(
                                text = "No Caffeine recorded",
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.tertiary)
                                        .padding(16.dp),
                                color = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    } else {
                        items(entries) { (_, drink) ->
                            DrinkItem(
                                drink = drink,
                                showTimestamp = true,
                                Modifier.clickable {
                                    showRemoveDialog = drink
                                },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            showRemoveDialog?.let { drink ->
                RemoveDrinkDialog(
                    drink = drink,
                    onConfirm = {
                        drink.timeConsumed?.let { timestamp ->
                            dbman.deleteEntry(timestamp, drink)
                            updateEntries()
                        }
                        showRemoveDialog = null
                    },
                    onDismiss = {
                        showRemoveDialog = null
                    },
                )
            }
        }
    }
}

@Composable
fun TodayScreen(
    onNavigateBack: () -> Unit = {},
    navController: NavController,
    dbman: MyDatabaseManager,
    updateEntries: () -> Unit,
) {
    val currentDate = remember { LocalDate.now() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM dd yyyy") }
    var selectedDate by rememberSaveable { mutableStateOf(currentDate) }
    val isToday = rememberSaveable(selectedDate) { selectedDate == LocalDate.now() }

    var entries by remember { mutableStateOf<List<CaffeineDrink>>(emptyList()) }
    var chartData by remember { mutableStateOf<List<CaffeinePoint>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val fetchDateData = { date: LocalDate ->
        scope.launch(Dispatchers.IO) {
            val dateEntries = dbman.readToday(date.toString())
            val dateChartData = CaffeineCalculator.calculateDayChart(dateEntries)
            withContext(Dispatchers.Main) {
                entries = dateEntries
                chartData = dateChartData
            }
        }
    }

    LaunchedEffect(selectedDate) {
        fetchDateData(selectedDate)
    }

    val isLandscape = LocalContext.current.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            CaffeinatedTopBar(
                showBackButton = true,
                onBackClick = onNavigateBack,
                onNavigateToScreen = { screen ->
                    navController.navigate(screen)
                },
            )
        },
    ) { innerPadding ->
        if (isLandscape) {
            TodayScreenLandscapeUI(
                innerPadding = innerPadding,
                isToday = isToday,
                selectedDate = selectedDate,
                dateFormatter = dateFormatter,
                currentDate = currentDate,
                drinks = entries,
                chartData = chartData,
                dailyLimit = CaffeineTracker.getDailyLimit(),
                onPreviousDay = {
                    selectedDate = selectedDate.minusDays(1)
                },
                onNextDay = {
                    if (!isToday) selectedDate = selectedDate.plusDays(1)
                },
                dbman = dbman,
                updateEntries = {
                    updateEntries()
                    fetchDateData(selectedDate)
                },
            )
        } else {
            TodayScreenUI(
                innerPadding = innerPadding,
                isToday = isToday,
                selectedDate = selectedDate,
                dateFormatter = dateFormatter,
                currentDate = currentDate,
                drinks = entries,
                chartData = chartData,
                dailyLimit = CaffeineTracker.getDailyLimit(),
                onPreviousDay = {
                    selectedDate = selectedDate.minusDays(1)
                },
                onNextDay = {
                    if (!isToday) selectedDate = selectedDate.plusDays(1)
                },
                dbman = dbman,
                updateEntries = {
                    updateEntries()
                    fetchDateData(selectedDate)
                },
            )
        }
    }
}

@Composable
fun TodayScreenLandscapeUI(
    innerPadding: PaddingValues,
    isToday: Boolean,
    selectedDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    currentDate: LocalDate,
    drinks: List<CaffeineDrink>,
    chartData: List<CaffeinePoint>,
    dailyLimit: Int,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    updateEntries: () -> Unit,
    dbman: MyDatabaseManager,
) {
    var showRemoveDialog by remember { mutableStateOf<CaffeineDrink?>(null) }
    val totalCaffeineConsumed = drinks.sumOf { it.caffeineMg }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = CenterVertically,
            ) {
                IconButton(onClick = onPreviousDay) {
                    Text("<", color = Color.White)
                }

                Text(
                    text = if (isToday) "Today" else selectedDate.format(dateFormatter),
                    color = Color.White,
                    fontSize = 20.sp,
                )

                IconButton(onClick = onNextDay, enabled = !isToday) {
                    Text(">", color = Color.White)
                }
            }
        }
        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.primary),
            ) {
                CaffeineChart(
                    chartData = chartData,
                    dailyLimit = dailyLimit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiary)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isToday) currentDate.format(dateFormatter) else selectedDate.format(dateFormatter),
                    color = MaterialTheme.colorScheme.onTertiary,
                )
                Text(
                    "$totalCaffeineConsumed mg",
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
        items(drinks) { drink ->
            DrinkItem(
                drink = drink,
                showTimestamp = true,
                modifier =
                    Modifier.clickable {
                        showRemoveDialog = drink
                    },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
        }
    }

    showRemoveDialog?.let { drink ->
        RemoveDrinkDialog(
            drink = drink,
            onConfirm = {
                drink.timeConsumed?.let { timestamp ->
                    dbman.deleteEntry(timestamp, drink)
                    updateEntries()
                }
                showRemoveDialog = null
            },
            onDismiss = {
                showRemoveDialog = null
            },
        )
    }
}

@Composable
fun TodayScreenUI(
    innerPadding: PaddingValues,
    isToday: Boolean,
    selectedDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    currentDate: LocalDate,
    drinks: List<CaffeineDrink>,
    chartData: List<CaffeinePoint>,
    dailyLimit: Int,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    updateEntries: () -> Unit,
    dbman: MyDatabaseManager,
) {
    var showRemoveDialog by remember { mutableStateOf<CaffeineDrink?>(null) }
    val totalCaffeineConsumed = drinks.sumOf { it.caffeineMg }

    Column(
        modifier = Modifier.fillMaxSize().padding(innerPadding).background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = CenterVertically,
        ) {
            IconButton(onClick = onPreviousDay) {
                Text("<", color = Color.White)
            }

            Text(
                text = if (isToday) "Today" else selectedDate.format(dateFormatter),
                color = Color.White,
                fontSize = 20.sp,
            )

            IconButton(onClick = onNextDay, enabled = !isToday) {
                Text(">", color = Color.White)
            }
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(MaterialTheme.colorScheme.primary),
        ) {
            CaffeineChart(
                chartData = chartData,
                dailyLimit = dailyLimit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (isToday) currentDate.format(dateFormatter) else selectedDate.format(dateFormatter),
                color = MaterialTheme.colorScheme.onTertiary,
            )
            Text(
                "$totalCaffeineConsumed mg",
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
        ) {
            items(drinks) { drink ->
                DrinkItem(
                    drink = drink,
                    showTimestamp = true,
                    modifier =
                        Modifier.clickable {
                            showRemoveDialog = drink
                        },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface)
            }
        }

        showRemoveDialog?.let { drink ->
            RemoveDrinkDialog(
                drink = drink,
                onConfirm = {
                    drink.timeConsumed?.let { timestamp ->
                        dbman.deleteEntry(timestamp, drink)
                        updateEntries()
                    }
                    showRemoveDialog = null
                },
                onDismiss = {
                    showRemoveDialog = null
                },
            )
        }
    }
}

@Composable
fun RemoveDrinkDialog(
    drink: CaffeineDrink,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Drink", color = MaterialTheme.colorScheme.onSecondary) },
        text = { Text("Would you like to remove ${drink.drink} (${drink.caffeineMg} mg)?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
fun CaffeineChart(
    chartData: List<CaffeinePoint>,
    dailyLimit: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(300.dp)
                .fillMaxWidth()
                .padding(36.dp),
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val highestCaffeineLevel = chartData.maxOfOrNull { it.amount } ?: 0.0
            val yAxisMax =
                when {
                    highestCaffeineLevel <= 200 -> 200.0
                    highestCaffeineLevel <= 300 -> 300.0
                    else -> ((highestCaffeineLevel + 99) / 100 * 100)
                }.coerceAtLeast(dailyLimit.toDouble())
            val stepSize = (yAxisMax / 4.0).roundToInt()
            val yAxisLabels = (0..4).map { it * stepSize }
            val xAxisLabels = listOf("6AM", "9AM", "12PM", "3PM", "6PM", "9PM")

            yAxisLabels.forEach { mg ->
                val y = size.height * (1 - (mg / yAxisMax))
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(size.width, y.toFloat()),
                    strokeWidth = 1f,
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "$mg",
                        -20f,
                        (y + 6f).toFloat(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 12.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        },
                    )
                }
            }

            xAxisLabels.forEachIndexed { index, time ->
                val x = size.width * (index / (xAxisLabels.size - 1).toFloat())
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        time,
                        x,
                        size.height + 50f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 12.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        },
                    )
                }
            }

            val limitY = size.height * (1 - (dailyLimit / yAxisMax))
            drawLine(
                color = Color.Red.copy(alpha = 0.5f),
                start = Offset(0f, limitY.toFloat()),
                end = Offset(size.width, limitY.toFloat()),
                strokeWidth = 2f,
            )

            if (chartData.isNotEmpty()) {
                val points =
                    chartData.mapIndexed { index, point ->
                        Offset(
                            x = size.width * (index.toFloat() / (chartData.size - 1)),
                            y = size.height * (1 - (point.amount / yAxisMax)).toFloat(),
                        )
                    }

                drawPath(
                    path =
                        Path().apply {
                            moveTo(points.first().x, points.first().y)
                            points.forEach { point ->
                                lineTo(point.x, point.y)
                            }
                        },
                    color = Color.White,
                    style =
                        Stroke(
                            width = 3f,
                            cap = StrokeCap.Round,
                        ),
                )

                points.forEachIndexed { index, point ->
                    if (index % 4 == 0) {
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = point,
                        )
                    }
                }
            }
        }
    }
}
