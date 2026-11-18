package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ai.configureAppCheck
import com.example.notifications.ReminderScheduler
import com.example.ui.MunazzemApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)
        ReminderScheduler.ensureChannel(this)

        enableEdgeToEdge()
        setContent {
            MunazzemApp()
        }
    }
}
