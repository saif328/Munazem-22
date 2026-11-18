package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "munazzem_prefs")

data class AppSettingsState(
    val darkTheme: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val cloudSyncConsent: Boolean = false,
    val aiDataContextConsent: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notifyLessons: Boolean = true,
    val notifyTasks: Boolean = true,
    val notifyReviews: Boolean = true,
    val notifyExams: Boolean = true,
    val notifyBacklog: Boolean = true,
    val reminderLeadMinutes: Int = 15,
    val pomodoroFocusMinutes: Int = 25,
    val pomodoroBreakMinutes: Int = 5
)

class AppPreferences(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CLOUD_SYNC_CONSENT = booleanPreferencesKey("cloud_sync_consent")
        val AI_DATA_CONTEXT_CONSENT = booleanPreferencesKey("ai_data_context_consent")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFY_LESSONS = booleanPreferencesKey("notify_lessons")
        val NOTIFY_TASKS = booleanPreferencesKey("notify_tasks")
        val NOTIFY_REVIEWS = booleanPreferencesKey("notify_reviews")
        val NOTIFY_EXAMS = booleanPreferencesKey("notify_exams")
        val NOTIFY_BACKLOG = booleanPreferencesKey("notify_backlog")
        val REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        val POMODORO_FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
        val POMODORO_BREAK_MINUTES = intPreferencesKey("pomodoro_break_minutes")
    }

    val settingsFlow: Flow<AppSettingsState> = context.dataStore.data.map { prefs ->
        AppSettingsState(
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            cloudSyncConsent = prefs[Keys.CLOUD_SYNC_CONSENT] ?: false,
            aiDataContextConsent = prefs[Keys.AI_DATA_CONTEXT_CONSENT] ?: true,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notifyLessons = prefs[Keys.NOTIFY_LESSONS] ?: true,
            notifyTasks = prefs[Keys.NOTIFY_TASKS] ?: true,
            notifyReviews = prefs[Keys.NOTIFY_REVIEWS] ?: true,
            notifyExams = prefs[Keys.NOTIFY_EXAMS] ?: true,
            notifyBacklog = prefs[Keys.NOTIFY_BACKLOG] ?: true,
            reminderLeadMinutes = prefs[Keys.REMINDER_LEAD_MINUTES] ?: 15,
            pomodoroFocusMinutes = prefs[Keys.POMODORO_FOCUS_MINUTES] ?: 25,
            pomodoroBreakMinutes = prefs[Keys.POMODORO_BREAK_MINUTES] ?: 5
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setCloudSyncConsent(consent: Boolean) {
        context.dataStore.edit { it[Keys.CLOUD_SYNC_CONSENT] = consent }
    }

    suspend fun setAiDataContextConsent(consent: Boolean) {
        context.dataStore.edit { it[Keys.AI_DATA_CONTEXT_CONSENT] = consent }
    }

    suspend fun updateNotificationSettings(
        enabled: Boolean,
        lessons: Boolean,
        tasks: Boolean,
        reviews: Boolean,
        exams: Boolean,
        backlog: Boolean,
        leadMinutes: Int
    ) {
        context.dataStore.edit {
            it[Keys.NOTIFICATIONS_ENABLED] = enabled
            it[Keys.NOTIFY_LESSONS] = lessons
            it[Keys.NOTIFY_TASKS] = tasks
            it[Keys.NOTIFY_REVIEWS] = reviews
            it[Keys.NOTIFY_EXAMS] = exams
            it[Keys.NOTIFY_BACKLOG] = backlog
            it[Keys.REMINDER_LEAD_MINUTES] = leadMinutes
        }
    }

    suspend fun updatePomodoroDefaults(focusMinutes: Int, breakMinutes: Int) {
        context.dataStore.edit {
            it[Keys.POMODORO_FOCUS_MINUTES] = focusMinutes.coerceIn(5, 180)
            it[Keys.POMODORO_BREAK_MINUTES] = breakMinutes.coerceIn(1, 60)
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
