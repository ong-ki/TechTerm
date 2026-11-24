package com.techterms.app

import android.app.Application
import android.util.Log
import androidx.work.ExistingWorkPolicy
import com.techterms.app.data.datastore.NotificationSettingsDataStore
import com.techterms.app.data.db.TechTermsDatabase
import com.techterms.app.data.repository.TermRepository
import com.techterms.app.notification.helper.NotificationHelper
import com.techterms.app.notification.helper.NotificationScheduler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database: TechTermsDatabase = TechTermsDatabase.getInstance(application)
    val settingsDataStore: NotificationSettingsDataStore = NotificationSettingsDataStore(application)
    val termRepository: TermRepository = TermRepository(
        context = application,
        termDao = database.termDao(),
        settingsDataStore = settingsDataStore
    )
}

class TechTermsApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled error in application scope", throwable)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        try {
            NotificationHelper.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel", e)
        }

        appScope.launch {
            try {
                container.termRepository.ensurePopulated()
                val settings = container.settingsDataStore.getSettings()
                if (settings.enabled) {
                    NotificationScheduler.schedule(
                        context = this@TechTermsApplication,
                        hour = settings.hour,
                        minute = settings.minute,
                        policy = ExistingWorkPolicy.KEEP
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Application background startup failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "TechTerms"
    }
}
