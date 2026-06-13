package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SocialMediaViewModel
import com.example.ui.screens.YarkhwoonApp
import com.example.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {
    companion object {
        var previousCrashLog: String? = null
    }

    private val viewModel: SocialMediaViewModel by viewModels()

    private fun writeCrashLog(throwable: Throwable) {
        try {
            val file = File(filesDir, "crash_log.txt")
            file.writeText(
                "Time: ${System.currentTimeMillis()}\n" +
                "Error: ${throwable.localizedMessage}\n" +
                "Stacktrace:\n${Log.getStackTraceString(throwable)}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Installation of a robust uncaught exception handler to make any runtime crashes visible in system logs and readable on next boot
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("YARKHWOON_CRASH", "Uncaught exception in thread '${thread.name}': ${throwable.localizedMessage}", throwable)
                writeCrashLog(throwable)
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable)
                } else {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    System.exit(10)
                }
            }
        }

        // Use a persistent crash-protection strategy for the Main Thread Looper as well as background threads
        var lastCrashTime = 0L
        var consecutiveCrashes = 0
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            while (true) {
                try {
                    android.os.Looper.loop()
                } catch (e: Throwable) {
                    Log.e("YARKHWOON_SAFELOOPER", "Main thread looper intercepted crash: ${e.localizedMessage}", e)
                    writeCrashLog(e)
                    
                    val now = System.currentTimeMillis()
                    if (now - lastCrashTime < 5000) {
                        consecutiveCrashes++
                    } else {
                        consecutiveCrashes = 1
                    }
                    lastCrashTime = now
                    
                    if (consecutiveCrashes > 5) {
                        Log.e("YARKHWOON_SYSTEM", "Infinite crash loop detected in UI thread. Terminating process cleanly.")
                        android.os.Process.killProcess(android.os.Process.myPid())
                        System.exit(10)
                    }
                }
            }
        }

        enableEdgeToEdge()

        val crashLogFile = File(filesDir, "crash_log.txt")
        if (crashLogFile.exists()) {
            try {
                previousCrashLog = crashLogFile.readText()
                Log.e("YARKHWOON_PREV_CRASH", "Detected and logged previous crash:\n$previousCrashLog")
                crashLogFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            MyApplicationTheme {
                var crashToShow by remember { mutableStateOf(previousCrashLog) }
                
                if (crashToShow != null) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Developer Diagnostics 🛠️",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "A crash occurred during the previous session. You can copy this technical breakdown or report it directly to help fix the issue immediately.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Crash Log Trace",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onError,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = crashToShow ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        previousCrashLog = null
                                        crashToShow = null
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text("Dismiss & Enter App")
                                }
                            }
                        }
                    }
                } else {
                    YarkhwoonApp(viewModel = viewModel)
                }
            }
        }
    }
}
