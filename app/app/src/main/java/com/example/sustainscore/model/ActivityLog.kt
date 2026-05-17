package com.example.sustainscore.model

data class ActivityLog(
    val id: String,
    val activityTypeId: String,
    val timestampMillis: Long,
    val amount: Double
)
