package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.model.BudgetLimit
import com.example.data.model.GameScoreRecord
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LocalService
import com.example.data.model.MoneyTransaction
import com.example.data.model.PersonalGoal
import com.example.data.model.PlannerTask
import com.example.data.model.SavedQuote
import com.example.data.model.ServiceReview
import com.example.data.model.ShoppingItem
import com.example.data.model.SmartReminder
import com.example.data.model.UserProfile
import com.example.data.repository.AppPreferences
import com.example.data.repository.BackupManager
import com.example.data.repository.DashboardConfig
import com.example.data.repository.FinanceRepository
import com.example.data.repository.GoalsAndBoostRepository
import com.example.data.repository.NotificationPreferences
import com.example.data.repository.PlannerRepository
import com.example.data.repository.ServicesRepository
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferencesRepository
import com.example.data.repository.unwrapFirestoreErrorMessage
import com.example.notifications.NotificationHelper
import com.example.util.SouthAfricaFormatters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class LifeHubViewModel(
    private val appContext: Context,
    private val userId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    private val financeRepo: FinanceRepository,
    private val plannerRepo: PlannerRepository,
    private val shoppingRepo: ShoppingRepository,
    private val goalsBoostRepo: GoalsAndBoostRepository,
    private val servicesRepo: ServicesRepository,
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _selectedServiceId = MutableStateFlow<String?>(null)
    val selectedServiceId: StateFlow<String?> = _selectedServiceId.asStateFlow()

    val preferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppPreferences())

    val profile: StateFlow<UserProfile?> = goalsBoostRepo.observeUserProfile(userId)
        .catch { Log.w(TAG, "Profile stream error", it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val transactionsState: StateFlow<UiState<List<MoneyTransaction>>> =
        financeRepo.observeTransactions(userId).toUiState("Failed to load transactions")

    val budgetsState: StateFlow<UiState<List<BudgetLimit>>> =
        financeRepo.observeBudgets(userId).toUiState("Failed to load budgets")

    val tasksState: StateFlow<UiState<List<PlannerTask>>> =
        plannerRepo.observeTasks(userId).toUiState("Failed to load tasks")

    val remindersState: StateFlow<UiState<List<SmartReminder>>> =
        plannerRepo.observeReminders(userId).toUiState("Failed to load reminders")

    val shoppingState: StateFlow<UiState<List<ShoppingItem>>> =
        shoppingRepo.observeShoppingItems(userId).toUiState("Failed to load shopping list")

    val goalsState: StateFlow<UiState<List<PersonalGoal>>> =
        goalsBoostRepo.observeGoals(userId).toUiState("Failed to load goals")

    val gameScoresState: StateFlow<UiState<List<GameScoreRecord>>> =
        goalsBoostRepo.observeGameScores(userId).toUiState("Failed to load game history")

    val savedQuotesState: StateFlow<UiState<List<SavedQuote>>> =
        goalsBoostRepo.observeSavedQuotes(userId).toUiState("Failed to load saved quotes")

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        goalsBoostRepo.observeLeaderboard().toUiState("Failed to load leaderboard")

    val servicesState: StateFlow<UiState<List<LocalService>>> =
        servicesRepo.observePublishedServices().toUiState("Failed to load local services")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedServiceReviewsState: StateFlow<UiState<List<ServiceReview>>> =
        _selectedServiceId.flatMapLatest { serviceId ->
            if (serviceId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                servicesRepo.observeReviews(serviceId).toUiStateFlow("Failed to load reviews")
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    init {
        viewModelScope.launch {
            goalsBoostRepo.ensureUserProfile(userId, userDisplayName, userEmail)
                .onFailure { Log.w(TAG, "Profile init error", it) }
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun showBanner(msg: String) {
        _statusBanner.value = msg
    }

    // --- Money Manager ---
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        dateIso: String,
        notes: String
    ) {
        viewModelScope.launch {
            financeRepo.addTransaction(userId, title, amount, type, category, dateIso, notes)
                .onSuccess { _statusBanner.value = "Saved ${SouthAfricaFormatters.formatZar(amount)} entry" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not save transaction") }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            financeRepo.deleteTransaction(userId, id)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not delete entry") }
        }
    }

    fun upsertBudget(category: String, limitAmount: Double, period: String, existingId: String? = null) {
        viewModelScope.launch {
            financeRepo.upsertBudget(userId, category, limitAmount, period, existingId)
                .onSuccess { _statusBanner.value = "Budget updated for $category" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not save budget") }
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            financeRepo.deleteBudget(userId, id)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not remove budget") }
        }
    }

    // --- Planner & Smart Reminders ---
    fun addTask(title: String, priority: String, dueDateIso: String, dueTime: String, category: String) {
        viewModelScope.launch {
            plannerRepo.addTask(userId, title, priority, dueDateIso, dueTime, category)
                .onSuccess { _statusBanner.value = "Task added" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not add task") }
        }
    }

    fun toggleTask(task: PlannerTask) {
        viewModelScope.launch {
            plannerRepo.toggleTaskCompleted(userId, task)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not update task") }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            plannerRepo.deleteTask(userId, id)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not delete task") }
        }
    }

    fun addReminder(
        title: String,
        category: String,
        dueDateTimeIso: String,
        recurrence: String,
        amountZar: Double,
        notificationsEnabled: Boolean
    ) {
        viewModelScope.launch {
            plannerRepo.addReminder(
                userId, title, category, dueDateTimeIso, recurrence, amountZar, notificationsEnabled
            ).onSuccess { newId ->
                if (notificationsEnabled && preferences.value.notifications.masterEnabled) {
                    NotificationHelper.scheduleReminderNotification(
                        appContext, newId, title, category, dueDateTimeIso
                    )
                }
                _statusBanner.value = "Reminder scheduled"
            }.onFailure {
                _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not schedule reminder")
            }
        }
    }

    fun toggleReminder(reminder: SmartReminder) {
        viewModelScope.launch {
            plannerRepo.toggleReminderCompleted(userId, reminder)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not update reminder") }
        }
    }

    fun deleteReminder(id: String) {
        viewModelScope.launch {
            plannerRepo.deleteReminder(userId, id)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not delete reminder") }
        }
    }

    // --- Shopping Assistant ---
    fun addShoppingItem(
        name: String,
        quantity: Double,
        unit: String,
        estimatedPrice: Double,
        shopName: String,
        category: String,
        isFrequent: Boolean
    ) {
        viewModelScope.launch {
            shoppingRepo.addShoppingItem(
                userId, name, quantity, unit, estimatedPrice, shopName, category, isFrequent
            ).onSuccess { _statusBanner.value = "Added $name to list" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not add item") }
        }
    }

    fun toggleShoppingPurchased(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepo.togglePurchased(userId, item)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not update item") }
        }
    }

    fun toggleShoppingFrequent(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepo.toggleFrequent(userId, item)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not update item") }
        }
    }

    fun reAddFrequentItem(item: ShoppingItem) {
        viewModelScope.launch {
            shoppingRepo.resetPurchasedForFrequent(userId, item)
                .onSuccess { _statusBanner.value = "${item.name} moved back to basket" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not re-add item") }
        }
    }

    fun deleteShoppingItem(id: String) {
        viewModelScope.launch {
            shoppingRepo.deleteShoppingItem(userId, id)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not remove item") }
        }
    }

    // --- Personal Goals, Morning Boost & Motivation ---
    fun addGoal(
        title: String,
        category: String,
        targetValue: Double,
        currentValue: Double,
        unitLabel: String,
        deadlineIso: String
    ) {
        viewModelScope.launch {
            goalsBoostRepo.addGoal(userId, title, category, targetValue, currentValue, unitLabel, deadlineIso)
                .onSuccess { _statusBanner.value = "Goal created!" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not create goal") }
        }
    }

    fun updateGoalProgress(goal: PersonalGoal, newCurrentValue: Double) {
        viewModelScope.launch {
            goalsBoostRepo.updateGoalProgress(userId, goal, newCurrentValue)
                .onSuccess {
                    if (newCurrentValue >= goal.targetValue && !goal.completed) {
                        _statusBanner.value = "Goal achieved: ${goal.title}!"
                    }
                }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not update goal") }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            goalsBoostRepo.deleteGoal(userId, id)
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not delete goal") }
        }
    }

    fun recordGameScore(gameId: String, difficulty: String, score: Int, durationSeconds: Int) {
        viewModelScope.launch {
            val name = profile.value?.displayName ?: userDisplayName
            val share = preferences.value.shareOnLeaderboard
            goalsBoostRepo.recordGameCompletion(
                userId, name, gameId, difficulty, score, durationSeconds, share
            ).onFailure {
                _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not sync game score")
            }
        }
    }

    fun toggleSaveQuote(quoteId: String, text: String, author: String, category: String, currentlySaved: Boolean) {
        viewModelScope.launch {
            goalsBoostRepo.toggleSaveQuote(userId, quoteId, text, author, category, currentlySaved)
                .onSuccess {
                    _statusBanner.value = if (currentlySaved) "Removed from favourites" else "Saved to favourites"
                }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not save quote") }
        }
    }

    // --- Local Services Directory ---
    fun selectServiceForReviews(serviceId: String?) {
        _selectedServiceId.value = serviceId
    }

    fun createLocalService(
        businessName: String,
        category: String,
        city: String,
        suburb: String,
        businessPhone: String,
        servicesOffered: String
    ) {
        viewModelScope.launch {
            val ownerName = profile.value?.displayName ?: userDisplayName
            servicesRepo.createService(
                ownerId = userId,
                ownerName = ownerName,
                businessName = businessName,
                category = category,
                city = city,
                suburb = suburb,
                businessPhone = businessPhone,
                servicesOffered = servicesOffered
            ).onSuccess {
                _statusBanner.value = "Business profile published to LIFEHUB Directory"
            }.onFailure {
                _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not publish business profile")
            }
        }
    }

    fun submitServiceReview(serviceId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            val authorName = profile.value?.displayName ?: userDisplayName
            servicesRepo.submitReview(serviceId, userId, authorName, rating, comment)
                .onSuccess { _statusBanner.value = "Thank you! Your genuine review was posted." }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not submit review") }
        }
    }

    fun deleteLocalService(serviceId: String) {
        viewModelScope.launch {
            servicesRepo.deleteService(serviceId)
                .onSuccess { _statusBanner.value = "Business listing removed" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not delete listing") }
        }
    }

    // --- Preferences, Theme, Backup & Recovery ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun updateDashboardConfig(config: DashboardConfig) {
        viewModelScope.launch { prefsRepo.updateDashboardConfig(config) }
    }

    fun updateNotificationPrefs(prefs: NotificationPreferences) {
        viewModelScope.launch { prefsRepo.updateNotificationPrefs(prefs) }
    }

    fun setShareOnLeaderboard(share: Boolean) {
        viewModelScope.launch { prefsRepo.setShareOnLeaderboard(share) }
    }

    fun updateSavingsTarget(displayName: String, targetZar: Double) {
        viewModelScope.launch {
            goalsBoostRepo.updateProfilePreferences(userId, displayName, targetZar)
                .onSuccess { _statusBanner.value = "Profile settings saved" }
                .onFailure { _statusBanner.value = unwrapFirestoreErrorMessage(it, "Could not update profile") }
        }
    }

    fun buildBackupJson(): String {
        val tx = (transactionsState.value as? UiState.Success)?.data.orEmpty()
        val tasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
        val rems = (remindersState.value as? UiState.Success)?.data.orEmpty()
        val shop = (shoppingState.value as? UiState.Success)?.data.orEmpty()
        val goals = (goalsState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            prefsRepo.setLastBackupIso(SouthAfricaFormatters.currentDateTimeDisplay())
        }
        return BackupManager.exportToJson(tx, tasks, rems, shop, goals)
    }

    fun restoreFromBackupJson(rawJson: String) {
        viewModelScope.launch {
            try {
                val payload = BackupManager.parseFromJson(rawJson)
                var count = 0
                payload.transactions.forEach {
                    if (financeRepo.addTransaction(userId, it.title, it.amount, it.type, it.category, it.dateIso.ifBlank { SouthAfricaFormatters.todayIso() }, it.notes).isSuccess) count++
                }
                payload.tasks.forEach {
                    if (plannerRepo.addTask(userId, it.title, it.priority, it.dueDateIso.ifBlank { SouthAfricaFormatters.todayIso() }, it.dueTime, it.category).isSuccess) count++
                }
                payload.reminders.forEach {
                    if (plannerRepo.addReminder(userId, it.title, it.category, it.dueDateTimeIso, it.recurrence, it.amountZar, it.notificationsEnabled).isSuccess) count++
                }
                payload.shoppingItems.forEach {
                    if (shoppingRepo.addShoppingItem(userId, it.name, it.quantity, it.unit, it.estimatedPrice, it.shopName, it.category, it.isFrequent).isSuccess) count++
                }
                payload.goals.forEach {
                    if (goalsBoostRepo.addGoal(userId, it.title, it.category, it.targetValue, it.currentValue, it.unitLabel, it.deadlineIso).isSuccess) count++
                }
                _statusBanner.value = "Restored $count records from backup"
            } catch (e: Exception) {
                _statusBanner.value = "Backup file invalid: ${e.localizedMessage ?: "unable to parse"}"
            }
        }
    }

    fun sendTestNotification() {
        NotificationHelper.showNotification(
            appContext,
            9001,
            "LIFEHUB SA Daily Check-In",
            "Notifications are active! Stay on top of your ZAR budget, bills, and Morning Boost streak."
        )
    }

    private fun <T> Flow<T>.toUiStateFlow(fallbackError: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, fallbackError, root)
                emit(UiState.Error(unwrapFirestoreErrorMessage(root, fallbackError)))
            }

    private fun <T> Flow<T>.toUiState(fallbackError: String): StateFlow<UiState<T>> =
        toUiStateFlow(fallbackError).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    companion object {
        private const val TAG = "LifeHubVM"

        fun provideFactory(
            context: Context,
            userId: String,
            displayName: String,
            email: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appCtx = context.applicationContext
                val dbId = appCtx.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId).apply {
                    runCatching {
                        firestoreSettings = FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(
                                PersistentCacheSettings.newBuilder()
                                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                                    .build()
                            )
                            .build()
                    }
                }
                return LifeHubViewModel(
                    appContext = appCtx,
                    userId = userId,
                    userDisplayName = displayName,
                    userEmail = email,
                    financeRepo = FinanceRepository(db),
                    plannerRepo = PlannerRepository(db),
                    shoppingRepo = ShoppingRepository(db),
                    goalsBoostRepo = GoalsAndBoostRepository(db),
                    servicesRepo = ServicesRepository(db),
                    prefsRepo = UserPreferencesRepository(appCtx)
                ) as T
            }
        }
    }
}
