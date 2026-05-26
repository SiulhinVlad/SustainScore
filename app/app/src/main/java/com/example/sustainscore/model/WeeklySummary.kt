package com.example.sustainscore.model

import java.time.LocalDate

data class WeeklySummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val highestImpactActivityName: String?,
    val averageScore: Double
)
