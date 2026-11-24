package com.techterms.app.notification.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import com.techterms.app.data.datastore.NotificationSettingsDataStore
import com.techterms.app.notification.helper.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = NotificationSettingsDataStore(appContext).getSettings()
                if (settings.enabled) {
                    NotificationScheduler.schedule(
                        context = appContext,
                        hour = settings.hour,
                        minute = settings.minute,
                        policy = ExistingWorkPolicy.REPLACE
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
