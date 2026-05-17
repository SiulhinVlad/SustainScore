package com.example.sustainscore.model

data class ActivityType(
    val id: String,
    val name: String,
    val unit: ActivityUnit,
    val weight: Double,
    val recommendedLimit: Double
)
