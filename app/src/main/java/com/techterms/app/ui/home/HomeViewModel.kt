package com.techterms.app.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import com.techterms.app.data.datastore.NotificationSettings
import com.techterms.app.data.datastore.NotificationSettingsDataStore
import com.techterms.app.data.datastore.ThemeMode
import com.techterms.app.data.db.Term
import com.techterms.app.data.repository.TermRepository
import com.techterms.app.notification.helper.NotificationScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val termOfTheDay: Term? = null,
    val snippetTerm: Term? = null,
    val notificationsEnabled: Boolean = NotificationSettings.DEFAULT_ENABLED,
    val notificationHour: Int = NotificationSettings.DEFAULT_HOUR,
    val notificationMinute: Int = NotificationSettings.DEFAULT_MINUTE,
    val devTermsOnly: Boolean = NotificationSettings.DEFAULT_DEV_TERMS_ONLY,
    val seedFailed: Boolean = false
)

class HomeViewModel(
    private val appContext: Context,
    private val termRepository: TermRepository,
    private val settingsDataStore: NotificationSettingsDataStore
) : ViewModel() {

    private val termOfTheDayFlow = MutableStateFlow<Term?>(null)
    private val snippetTermFlow = MutableStateFlow<Term?>(null)
    private val isLoadingFlow = MutableStateFlow(true)

    val uiState: StateFlow<HomeUiState> = combine(
        isLoadingFlow,
        termOfTheDayFlow,
        snippetTermFlow,
        settingsDataStore.settingsFlow,
        termRepository.seedFailed
    ) { isLoading, term, snippetTerm, settings, seedFailed ->
        HomeUiState(
            isLoading = isLoading,
            termOfTheDay = term,
            snippetTerm = snippetTerm,
            notificationsEnabled = settings.enabled,
            notificationHour = settings.hour,
            notificationMinute = settings.minute,
            devTermsOnly = settings.devTermsOnly,
            seedFailed = seedFailed
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HomeUiState()
    )

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = ThemeMode.SYSTEM
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                settingsDataStore.setThemeMode(mode)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save theme mode", e)
            }
        }
    }

    val stats: StateFlow<HomeStats> = termRepository.getAllTerms()
        .map { terms -> HomeStats.from(terms) }
        .catch { e ->
            Log.e(TAG, "Failed to compute home stats", e)
            emit(HomeStats())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeStats()
        )

    init {
        viewModelScope.launch {
            settingsDataStore.settingsFlow
                .map { it.devTermsOnly }
                .distinctUntilChanged()
                .catch { e ->
                    Log.e(TAG, "Error observing settings in HomeViewModel", e)
                    emit(NotificationSettings.DEFAULT_DEV_TERMS_ONLY)
                }
                .collectLatest { devOnly ->
                    isLoadingFlow.value = true
                    try {
                        val dailyTerm = termRepository.getTermOfTheDay(devTermsOnly = devOnly)
                        val resolvedSnippetTerm = if (!dailyTerm?.codeSnippet.isNullOrBlank()) {
                            dailyTerm
                        } else {
                            termRepository.getTermOfTheDay(devTermsOnly = true)
                                ?.takeIf { !it.codeSnippet.isNullOrBlank() }
                        }
                        termOfTheDayFlow.value = dailyTerm
                        snippetTermFlow.value = resolvedSnippetTerm
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load Home screen terms", e)
                        termOfTheDayFlow.value = null
                        snippetTermFlow.value = null
                    } finally {
                        isLoadingFlow.value = false
                    }
                }
        }
    }

    fun setNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsDataStore.setEnabled(enabled)
                val current = settingsDataStore.getSettings()
                if (enabled) {
                    NotificationScheduler.schedule(
                        context = appContext,
                        hour = current.hour,
                        minute = current.minute,
                        policy = ExistingWorkPolicy.REPLACE
                    )
                } else {
                    NotificationScheduler.cancel(appContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification enabled state", e)
            }
        }
    }

    fun setNotificationTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            try {
                settingsDataStore.setTime(hour, minute)
                val current = settingsDataStore.getSettings()
                if (current.enabled) {
                    NotificationScheduler.schedule(
                        context = appContext,
                        hour = current.hour,
                        minute = current.minute,
                        policy = ExistingWorkPolicy.REPLACE
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update notification time", e)
            }
        }
    }

    fun setDevTermsOnly(devOnly: Boolean) {
        viewModelScope.launch {
            try {
                settingsDataStore.setDevTermsOnly(devOnly)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update devTermsOnly preference", e)
            }
        }
    }

    companion object {
        private const val TAG = "TechTerms"

        fun provideFactory(
            appContext: Context,
            termRepository: TermRepository,
            settingsDataStore: NotificationSettingsDataStore
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    appContext = appContext.applicationContext,
                    termRepository = termRepository,
                    settingsDataStore = settingsDataStore
                ) as T
            }
        }
    }
}
