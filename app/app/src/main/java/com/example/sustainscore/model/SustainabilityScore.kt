package com.example.sustainscore.model

data class SustainabilityScore(
    val date: String,
    val hour: Int,
    val actualScore: Double,
    val recommendedScore: Double
)
