package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SocialMediaViewModel
import com.example.ui.screens.YarkhwoonApp
import com.example.ui.theme.YarkhoonTheme

class MainActivity : ComponentActivity() {
    companion object {
        var previousCrashLog: String? = null
    }

    private val socialMediaViewModel: SocialMediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Capture crash logs to display diagnostics dialog on next launch if needed
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            previousCrashLog = "${throwable.javaClass.simpleName}: ${throwable.message}\n${throwable.stackTrace.take(6).joinToString("\n")}"
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val isDarkMode by socialMediaViewModel.isDarkMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            YarkhoonTheme(darkTheme = isDarkMode ?: systemDark) {
                YarkhwoonApp(viewModel = socialMediaViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra("from_fcm", false)) {
            val type = intent.getStringExtra("notification_type")
            val targetId = intent.getStringExtra("target_id")
            socialMediaViewModel.handleNotificationRoute(type, targetId)
        }

        intent.data?.let { uri ->
            socialMediaViewModel.handleDeepLink(uri)
        }
    }
}
