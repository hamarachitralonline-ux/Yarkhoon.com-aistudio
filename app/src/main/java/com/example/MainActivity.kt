package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.screens.YarkhoonMainApp
import com.example.ui.theme.YarkhoonTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private var activeConversationId by mutableStateOf<String?>(null)
    private var activeOpenScreen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase safely
        try {
            val app = FirebaseApp.initializeApp(this)
            Log.d("YarkhoonApp", "Firebase initialized successfully: ${app?.name ?: "default"}")
        } catch (e: Exception) {
            Log.e("YarkhoonApp", "Firebase initialization notice: ${e.message}", e)
        }

        handleIncomingIntent(intent)

        enableEdgeToEdge()
        setContent {
            YarkhoonTheme {
                YarkhoonMainApp(
                    initialConversationId = activeConversationId,
                    initialOpenScreen = activeOpenScreen
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val convId = intent.getStringExtra("conversationId")
        val screen = intent.getStringExtra("openScreen")
        val notifType = intent.getStringExtra("notificationType")

        Log.d("YarkhoonApp", "Incoming notification intent: type=$notifType, convId=$convId, screen=$screen")

        if (!convId.isNullOrBlank()) {
            activeConversationId = convId
        }
        if (!screen.isNullOrBlank()) {
            activeOpenScreen = screen
        } else if (notifType == "friend_request" || notifType == "follow_request") {
            activeOpenScreen = "notifications"
        }
    }
}
