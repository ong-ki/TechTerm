package com.techterms.app.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.techterms.app.TechTermsApplication
import com.techterms.app.notification.helper.NotificationScheduler

/** Periodic safety net: re-creates the daily work if the chain was lost (KEEP is a no-op otherwise). */
class SafetyRescheduleWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TechTermsApplication ?: return Result.failure()
        val settings = app.container.settingsDataStore.getSettings()
        if (settings.enabled) {
            NotificationScheduler.schedule(
                context = applicationContext,
                hour = settings.hour,
                minute = settings.minute,
                policy = ExistingWorkPolicy.KEEP,
                withSafetyNet = false
            )
        }
        return Result.success()
    }
}
