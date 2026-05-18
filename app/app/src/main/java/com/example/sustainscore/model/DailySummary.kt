package com.example.sustainscore.model

data class DailySummary(
    val date: String,
    val averageScore: Double,
    val bestHour: Int?,
    val worstHour: Int?,
    val highestImpactActivityName: String?
)
