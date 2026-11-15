package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lifeHubDataStore: DataStore<Preferences> by preferencesDataStore(name = "lifehub_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class NotificationFrequency { IMMEDIATE, DAILY_DIGEST, WEEKLY_SUMMARY }

data class DashboardConfig(
    val showMoney: Boolean = true,
    val showTasks: Boolean = true,
    val showReminders: Boolean = true,
    val showShopping: Boolean = true,
    val showGoals: Boolean = true,
    val showMorningBoost: Boolean = true,
    val showMotivation: Boolean = true
)

data class NotificationPreferences(
    val masterEnabled: Boolean = true,
    val billReminders: Boolean = true,
    val taskDeadlines: Boolean = true,
    val shoppingReminders: Boolean = true,
    val dailyBoostChallenges: Boolean = true,
    val goalReminders: Boolean = true,
    val frequency: NotificationFrequency = NotificationFrequency.IMMEDIATE
)

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dashboard: DashboardConfig = DashboardConfig(),
    val notifications: NotificationPreferences = NotificationPreferences(),
    val shareOnLeaderboard: Boolean = true,
    val lastBackupIso: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_MONEY = booleanPreferencesKey("dash_money")
        val SHOW_TASKS = booleanPreferencesKey("dash_tasks")
        val SHOW_REMINDERS = booleanPreferencesKey("dash_reminders")
        val SHOW_SHOPPING = booleanPreferencesKey("dash_shopping")
        val SHOW_GOALS = booleanPreferencesKey("dash_goals")
        val SHOW_BOOST = booleanPreferencesKey("dash_boost")
        val SHOW_MOTIVATION = booleanPreferencesKey("dash_motivation")

        val NOTIF_MASTER = booleanPreferencesKey("notif_master")
        val NOTIF_BILLS = booleanPreferencesKey("notif_bills")
        val NOTIF_TASKS = booleanPreferencesKey("notif_tasks")
        val NOTIF_SHOPPING = booleanPreferencesKey("notif_shopping")
        val NOTIF_BOOST = booleanPreferencesKey("notif_boost")
        val NOTIF_GOALS = booleanPreferencesKey("notif_goals")
        val NOTIF_FREQ = stringPreferencesKey("notif_freq")

        val SHARE_LEADERBOARD = booleanPreferencesKey("share_leaderboard")
        val LAST_BACKUP = stringPreferencesKey("last_backup_iso")
    }

    val preferencesFlow: Flow<AppPreferences> = context.lifeHubDataStore.data.map { prefs ->
        AppPreferences(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            dashboard = DashboardConfig(
                showMoney = prefs[Keys.SHOW_MONEY] ?: true,
                showTasks = prefs[Keys.SHOW_TASKS] ?: true,
                showReminders = prefs[Keys.SHOW_REMINDERS] ?: true,
                showShopping = prefs[Keys.SHOW_SHOPPING] ?: true,
                showGoals = prefs[Keys.SHOW_GOALS] ?: true,
                showMorningBoost = prefs[Keys.SHOW_BOOST] ?: true,
                showMotivation = prefs[Keys.SHOW_MOTIVATION] ?: true
            ),
            notifications = NotificationPreferences(
                masterEnabled = prefs[Keys.NOTIF_MASTER] ?: true,
                billReminders = prefs[Keys.NOTIF_BILLS] ?: true,
                taskDeadlines = prefs[Keys.NOTIF_TASKS] ?: true,
                shoppingReminders = prefs[Keys.NOTIF_SHOPPING] ?: true,
                dailyBoostChallenges = prefs[Keys.NOTIF_BOOST] ?: true,
                goalReminders = prefs[Keys.NOTIF_GOALS] ?: true,
                frequency = runCatching {
                    NotificationFrequency.valueOf(
                        prefs[Keys.NOTIF_FREQ] ?: NotificationFrequency.IMMEDIATE.name
                    )
                }.getOrDefault(NotificationFrequency.IMMEDIATE)
            ),
            shareOnLeaderboard = prefs[Keys.SHARE_LEADERBOARD] ?: true,
            lastBackupIso = prefs[Keys.LAST_BACKUP] ?: ""
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.lifeHubDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateDashboardConfig(config: DashboardConfig) {
        context.lifeHubDataStore.edit { prefs ->
            prefs[Keys.SHOW_MONEY] = config.showMoney
            prefs[Keys.SHOW_TASKS] = config.showTasks
            prefs[Keys.SHOW_REMINDERS] = config.showReminders
            prefs[Keys.SHOW_SHOPPING] = config.showShopping
            prefs[Keys.SHOW_GOALS] = config.showGoals
            prefs[Keys.SHOW_BOOST] = config.showMorningBoost
            prefs[Keys.SHOW_MOTIVATION] = config.showMotivation
        }
    }

    suspend fun updateNotificationPrefs(notif: NotificationPreferences) {
        context.lifeHubDataStore.edit { prefs ->
            prefs[Keys.NOTIF_MASTER] = notif.masterEnabled
            prefs[Keys.NOTIF_BILLS] = notif.billReminders
            prefs[Keys.NOTIF_TASKS] = notif.taskDeadlines
            prefs[Keys.NOTIF_SHOPPING] = notif.shoppingReminders
            prefs[Keys.NOTIF_BOOST] = notif.dailyBoostChallenges
            prefs[Keys.NOTIF_GOALS] = notif.goalReminders
            prefs[Keys.NOTIF_FREQ] = notif.frequency.name
        }
    }

    suspend fun setShareOnLeaderboard(share: Boolean) {
        context.lifeHubDataStore.edit { it[Keys.SHARE_LEADERBOARD] = share }
    }

    suspend fun setLastBackupIso(timestamp: String) {
        context.lifeHubDataStore.edit { it[Keys.LAST_BACKUP] = timestamp }
    }
}
