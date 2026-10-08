package app.tidelio.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.tidelio.AppContainer
import app.tidelio.R
import app.tidelio.domain.goals.GoalApplyFrom
import app.tidelio.ui.calendar.CalendarScreen
import app.tidelio.ui.calendar.CalendarViewModel
import app.tidelio.ui.entry.EntryFormScreen
import app.tidelio.ui.entry.EntryFormViewModel
import app.tidelio.ui.settings.PrivacyScreen
import app.tidelio.ui.settings.SettingsScreen
import app.tidelio.ui.settings.SettingsViewModel
import app.tidelio.ui.settings.SetupScreen
import app.tidelio.ui.statistics.StatisticsScreen
import app.tidelio.ui.statistics.StatisticsViewModel
import app.tidelio.ui.wave.DayScreen
import app.tidelio.ui.wave.DayViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

object Routes {
    const val SETUP = "setup"
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val DAY = "day/{epochDay}"
    const val ENTRY = "entry?entryId={entryId}&date={date}"

    fun day(date: LocalDate) = "day/${date.toEpochDay()}"
    fun newEntry(date: LocalDate) = "entry?entryId=-1&date=${date.toEpochDay()}"
    fun editEntry(id: Long) = "entry?entryId=$id&date=${Long.MIN_VALUE}"
}

private data class TopLevel(val route: String, val label: String, val icon: Int)

private val topLevels = listOf(
    TopLevel(Routes.TODAY, "Today", R.drawable.ic_nav_today),
    TopLevel(Routes.CALENDAR, "Calendar", R.drawable.ic_nav_calendar),
    TopLevel(Routes.STATS, "Statistics", R.drawable.ic_nav_stats),
    TopLevel(Routes.SETTINGS, "Settings", R.drawable.ic_nav_settings),
)

@Composable
fun TidelioRoot(container: AppContainer, startWithSetup: Boolean) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = topLevels.any { it.route == currentRoute }

    LaunchedEffect(container) {
        container.undo.messages.collectLatest { message ->
            val result = snackbar.showSnackbar(
                message = message.text,
                actionLabel = "Undo",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) container.undo.performUndo(message)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController, currentRoute)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startWithSetup) Routes.SETUP else Routes.TODAY,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.SETUP) {
                val finish: () -> Unit = {
                    navController.navigate(Routes.TODAY) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
                SetupScreen(
                    onGoalChosen = { goal ->
                        scope.launch {
                            container.setGoal(goal, GoalApplyFrom.TODAY)
                            container.preferences.setSetupDone(true)
                            finish()
                        }
                    },
                    onSkip = {
                        scope.launch {
                            container.preferences.setSetupDone(true)
                            finish()
                        }
                    },
                )
            }
            composable(Routes.TODAY) {
                val vm: DayViewModel = viewModel(
                    factory = viewModelFactory { initializer { DayViewModel(container, createSavedStateHandle(), null) } },
                )
                DayScreen(
                    viewModel = vm,
                    isTodayScreen = true,
                    onAddEntry = { navController.navigate(Routes.newEntry(it)) },
                    onOpenEntry = { navController.navigate(Routes.editEntry(it)) },
                    onSetGoal = { ml, from ->
                        scope.launch { container.setGoal(ml, from) }
                    },
                    onBack = null,
                )
            }
            composable(
                Routes.DAY,
                arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
            ) { entry ->
                val epochDay = entry.arguments?.getLong("epochDay") ?: container.today().toEpochDay()
                val vm: DayViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { DayViewModel(container, createSavedStateHandle(), LocalDate.ofEpochDay(epochDay)) }
                    },
                )
                DayScreen(
                    viewModel = vm,
                    isTodayScreen = false,
                    onAddEntry = { navController.navigate(Routes.newEntry(it)) },
                    onOpenEntry = { navController.navigate(Routes.editEntry(it)) },
                    onSetGoal = { ml, from ->
                        scope.launch { container.setGoal(ml, from) }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Routes.ENTRY,
                arguments = listOf(
                    navArgument("entryId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("date") {
                        type = NavType.LongType
                        defaultValue = Long.MIN_VALUE
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("entryId")?.takeIf { it >= 0 }
                val date = entry.arguments?.getLong("date")?.takeIf { it != Long.MIN_VALUE }?.let { LocalDate.ofEpochDay(it) }
                val vm: EntryFormViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer { EntryFormViewModel(container, createSavedStateHandle(), id, date) }
                    },
                )
                EntryFormScreen(viewModel = vm, onClose = { navController.popBackStack() })
            }
            composable(Routes.CALENDAR) {
                val vm: CalendarViewModel = viewModel(
                    factory = viewModelFactory { initializer { CalendarViewModel(container, createSavedStateHandle()) } },
                )
                CalendarScreen(viewModel = vm, onOpenDay = { navController.navigate(Routes.day(it)) })
            }
            composable(Routes.STATS) {
                val vm: StatisticsViewModel = viewModel(
                    factory = viewModelFactory { initializer { StatisticsViewModel(container, createSavedStateHandle()) } },
                )
                StatisticsScreen(viewModel = vm)
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = viewModelFactory { initializer { SettingsViewModel(container) } },
                )
                SettingsScreen(
                    viewModel = vm,
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onDataCleared = {
                        navController.navigate(Routes.SETUP) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        topLevels.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Today is always the root of the main back stack (setup is popped when finished).
                            popUpTo(Routes.TODAY) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(painterResource(item.icon), contentDescription = null) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
}
