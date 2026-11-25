package com.techterms.app.notification.helper

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.techterms.app.notification.worker.DailyTermWorker
import com.techterms.app.notification.worker.SafetyRescheduleWorker
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val UNIQUE_WORK_NAME = "daily_term_notification_work"
    private const val SAFETY_WORK_NAME = "daily_term_safety_check_work"

    fun computeInitialDelayMillis(
        hour: Int,
        minute: Int,
        now: ZonedDateTime = ZonedDateTime.now(),
        forceNextDay: Boolean = false
    ): Long {
        var nextTrigger = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)

        if (forceNextDay || !nextTrigger.isAfter(now)) {
            nextTrigger = nextTrigger.plusDays(1)
        }

        return Duration.between(now, nextTrigger).toMillis().coerceAtLeast(1_000L)
    }

    fun schedule(
        context: Context,
        hour: Int,
        minute: Int,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
        forceNextDay: Boolean = false,
        withSafetyNet: Boolean = true
    ) {
        val delayMillis = computeInitialDelayMillis(
            hour = hour,
            minute = minute,
            forceNextDay = forceNextDay
        )

        val request = OneTimeWorkRequestBuilder<DailyTermWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_WORK_NAME, policy, request)

        if (withSafetyNet) {
            val safety = PeriodicWorkRequestBuilder<SafetyRescheduleWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(SAFETY_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, safety)
        }
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(SAFETY_WORK_NAME)
    }
}
