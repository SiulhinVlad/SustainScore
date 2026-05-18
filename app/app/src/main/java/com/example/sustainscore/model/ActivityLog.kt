package com.example.sustainscore.model

data class ActivityLog(
    val id: String,
    val activityTypeId: String,
    val startTimestampMillis: Long,
    val quantity: Double,
    val durationMinutes: Int
) {
    val durationHours: Double
        get() = durationMinutes / 60.0

    val quantityHours: Double
        get() = quantity * durationHours
}
