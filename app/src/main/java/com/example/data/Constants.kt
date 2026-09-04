package com.example.data

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppColors {
    val FacebookBlue = Color(0xFF1877F2)
    val DarkBackground = Color(0xFF121212)
    val LightSurface = Color(0xFFF0F2F5)
    val MessengerBubbleMe = Color(0xFF0084FF)
    val MessengerBubbleThem = Color(0xFFE4E6EB)
}

object Constants {
    fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
