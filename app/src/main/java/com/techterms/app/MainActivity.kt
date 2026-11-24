package com.techterms.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.techterms.app.notification.helper.NotificationHelper
import com.techterms.app.ui.navigation.TechTermsNavGraph
import com.techterms.app.ui.theme.TechTermsTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val deepLinkTermId = MutableStateFlow<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            extractTermIdFromIntent(intent)
        }

        val container = (application as? TechTermsApplication)?.container
            ?: AppContainer(application)

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val pendingTermId by deepLinkTermId.collectAsStateWithLifecycle()

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            TechTermsTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TechTermsNavGraph(
                        container = container,
                        deepLinkTermId = pendingTermId,
                        onDeepLinkConsumed = { deepLinkTermId.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTermIdFromIntent(intent)
    }

    private fun extractTermIdFromIntent(intent: Intent?) {
        if (intent == null) return
        try {
            if (!intent.hasExtra(NotificationHelper.EXTRA_TERM_ID)) return
            val termId = intent.getIntExtra(NotificationHelper.EXTRA_TERM_ID, -1)
            if (termId > 0) {
                deepLinkTermId.value = termId
            }
            intent.removeExtra(NotificationHelper.EXTRA_TERM_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read notification intent extra", e)
        }
    }

    companion object {
        private const val TAG = "TechTerms"
    }
}
