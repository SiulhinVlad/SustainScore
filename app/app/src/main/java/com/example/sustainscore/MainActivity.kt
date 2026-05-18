package com.example.sustainscore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sustainscore.ui.dashboard.DashboardRoute
import com.example.sustainscore.ui.theme.SustainScoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SustainScoreTheme {
                DashboardRoute()
            }
        }
    }
}
