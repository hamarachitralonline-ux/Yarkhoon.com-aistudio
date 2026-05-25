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
        
        // Install a robust uncaught exception handler to make any runtime crashes visible in system logs
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("YARKHWOON_CRASH", "Uncaught exception in thread '${thread.name}': ${throwable.localizedMessage}", throwable)
            writeCrashLog(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        enableEdgeToEdge()

        val crashLogFile = File(filesDir, "crash_log.txt")
        if (crashLogFile.exists()) {
            try {
                val crashDetails = crashLogFile.readText()
                Log.e("YARKHWOON_PREV_CRASH", "Detected and logged previous crash:\n$crashDetails")
                crashLogFile.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            MyApplicationTheme {
                YarkhwoonApp(viewModel = viewModel)
            }
        }
    }
}
