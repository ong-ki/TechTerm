package com.techterms.app.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.techterms.app.TechTermsApplication
import com.techterms.app.notification.helper.NotificationHelper
import com.techterms.app.notification.helper.NotificationScheduler

class DailyTermWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TechTermsApplication ?: return Result.failure()
        val settings = app.container.settingsDataStore.getSettings()
        if (!settings.enabled) {
            return Result.success()
        }

        val termOfTheDay = app.container.termRepository.getTermOfTheDay(
            devTermsOnly = settings.devTermsOnly
        )
        if (termOfTheDay != null) {
            NotificationHelper.showDailyTermNotification(applicationContext, termOfTheDay)
        }

        // Reschedule for the next day at the user's chosen time.
        NotificationScheduler.schedule(
            context = applicationContext,
            hour = settings.hour,
            minute = settings.minute,
            policy = ExistingWorkPolicy.REPLACE,
            forceNextDay = true
        )

        return Result.success()
    }
}
