package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.ThemeMode
import com.example.notifications.NotificationHelper
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExploreHubScreen
import com.example.ui.screens.ExploreSection
import com.example.ui.screens.MoneyScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.ShoppingScreen
import com.example.ui.theme.LifeHubTheme
import com.example.ui.viewmodel.LifeHubViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

enum class RootTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    MONEY("Money", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_money"),
    PLAN("Plan", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_plan"),
    SHOP("Shop", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "nav_shop"),
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureChannel(this)
        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        LifeHubTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedApp(
                userId = user.uid,
                displayName = user.displayName ?: "Friend",
                email = user.email ?: ""
            )
        }
    }
}

@Composable
fun AuthenticatedApp(
    userId: String,
    displayName: String,
    email: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: LifeHubViewModel = viewModel(
        key = userId,
        factory = LifeHubViewModel.provideFactory(context, userId, displayName, email)
    )

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
    val budgetsState by viewModel.budgetsState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val remindersState by viewModel.remindersState.collectAsStateWithLifecycle()
    val shoppingState by viewModel.shoppingState.collectAsStateWithLifecycle()
    val goalsState by viewModel.goalsState.collectAsStateWithLifecycle()
    val gameScoresState by viewModel.gameScoresState.collectAsStateWithLifecycle()
    val savedQuotesState by viewModel.savedQuotesState.collectAsStateWithLifecycle()
    val leaderboardState by viewModel.leaderboardState.collectAsStateWithLifecycle()
    val servicesState by viewModel.servicesState.collectAsStateWithLifecycle()
    val reviewsState by viewModel.selectedServiceReviewsState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()

    val darkTheme = when (prefs.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var currentTab by remember { mutableStateOf(RootTab.HOME) }
    var exploreSection by remember { mutableStateOf(ExploreSection.OVERVIEW) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val budgets = (budgetsState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val reminders = (remindersState as? UiState.Success)?.data.orEmpty()
    val shoppingItems = (shoppingState as? UiState.Success)?.data.orEmpty()
    val goals = (goalsState as? UiState.Success)?.data.orEmpty()
    val gameScores = (gameScoresState as? UiState.Success)?.data.orEmpty()
    val savedQuotes = (savedQuotesState as? UiState.Success)?.data.orEmpty()
    val leaderboard = (leaderboardState as? UiState.Success)?.data.orEmpty()
    val services = (servicesState as? UiState.Success)?.data.orEmpty()
    val reviews = (reviewsState as? UiState.Success)?.data.orEmpty()

    LifeHubTheme(darkTheme = darkTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar {
                            RootTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        currentTab = tab
                                        if (tab == RootTab.EXPLORE) exploreSection = ExploreSection.OVERVIEW
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail {
                            RootTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        currentTab = tab
                                        if (tab == RootTab.EXPLORE) exploreSection = ExploreSection.OVERVIEW
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.widthIn(max = 860.dp).fillMaxSize()) {
                            when (currentTab) {
                                RootTab.HOME -> DashboardScreen(
                                    displayName = profile?.displayName ?: displayName,
                                    profile = profile,
                                    dashboardConfig = prefs.dashboard,
                                    transactions = transactions,
                                    tasks = tasks,
                                    reminders = reminders,
                                    shoppingItems = shoppingItems,
                                    goals = goals,
                                    savedQuotes = savedQuotes,
                                    onUpdateDashboardConfig = viewModel::updateDashboardConfig,
                                    onToggleTask = viewModel::toggleTask,
                                    onToggleSaveQuote = viewModel::toggleSaveQuote,
                                    onNavigateToMoney = { currentTab = RootTab.MONEY },
                                    onNavigateToPlan = { currentTab = RootTab.PLAN },
                                    onNavigateToShop = { currentTab = RootTab.SHOP },
                                    onNavigateToMorningBoost = {
                                        currentTab = RootTab.EXPLORE
                                        exploreSection = ExploreSection.MORNING_BOOST
                                    },
                                    onNavigateToMotivation = {
                                        currentTab = RootTab.EXPLORE
                                        exploreSection = ExploreSection.MOTIVATION
                                    }
                                )

                                RootTab.MONEY -> MoneyScreen(
                                    transactions = transactions,
                                    budgets = budgets,
                                    savingsTargetZar = profile?.savingsTargetZar ?: 5000.0,
                                    onAddTransaction = viewModel::addTransaction,
                                    onDeleteTransaction = viewModel::deleteTransaction,
                                    onUpsertBudget = { cat, lim, per -> viewModel.upsertBudget(cat, lim, per) },
                                    onDeleteBudget = viewModel::deleteBudget
                                )

                                RootTab.PLAN -> PlannerScreen(
                                    tasks = tasks,
                                    reminders = reminders,
                                    goals = goals,
                                    onAddTask = viewModel::addTask,
                                    onToggleTask = viewModel::toggleTask,
                                    onDeleteTask = viewModel::deleteTask,
                                    onAddReminder = viewModel::addReminder,
                                    onToggleReminder = viewModel::toggleReminder,
                                    onDeleteReminder = viewModel::deleteReminder,
                                    onAddGoal = viewModel::addGoal,
                                    onUpdateGoalProgress = viewModel::updateGoalProgress,
                                    onDeleteGoal = viewModel::deleteGoal
                                )

                                RootTab.SHOP -> ShoppingScreen(
                                    items = shoppingItems,
                                    onAddItem = viewModel::addShoppingItem,
                                    onTogglePurchased = viewModel::toggleShoppingPurchased,
                                    onToggleFrequent = viewModel::toggleShoppingFrequent,
                                    onReAddFrequent = viewModel::reAddFrequentItem,
                                    onDeleteItem = viewModel::deleteShoppingItem
                                )

                                RootTab.EXPLORE -> ExploreHubScreen(
                                    section = exploreSection,
                                    onSectionChange = { exploreSection = it },
                                    currentUserId = userId,
                                    profile = profile,
                                    preferences = prefs,
                                    gameScores = gameScores,
                                    leaderboard = leaderboard,
                                    savedQuotes = savedQuotes,
                                    services = services,
                                    selectedServiceReviews = reviews,
                                    onSelectServiceForReviews = viewModel::selectServiceForReviews,
                                    onGameCompleted = viewModel::recordGameScore,
                                    onToggleSaveQuote = viewModel::toggleSaveQuote,
                                    onCreateService = viewModel::createLocalService,
                                    onSubmitReview = viewModel::submitServiceReview,
                                    onDeleteService = viewModel::deleteLocalService,
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onUpdateNotificationPrefs = viewModel::updateNotificationPrefs,
                                    onSetShareOnLeaderboard = viewModel::setShareOnLeaderboard,
                                    onUpdateProfile = viewModel::updateSavingsTarget,
                                    onBuildBackupJson = viewModel::buildBackupJson,
                                    onRestoreBackupJson = viewModel::restoreFromBackupJson,
                                    onSendTestNotification = viewModel::sendTestNotification,
                                    onShowMessage = viewModel::showBanner,
                                    onNavigateToGoals = { currentTab = RootTab.PLAN },
                                    onSignOut = {
                                        signOutUser(credentialManager, scope)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
