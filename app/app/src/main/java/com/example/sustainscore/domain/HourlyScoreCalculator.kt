package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.SustainabilityScore
import java.util.Calendar

class HourlyScoreCalculator(
    private val scoreCalculator: ScoreCalculator
) {
    fun calculate(date: String, hour: Int, logs: List<ActivityLog>): SustainabilityScore {
        val hourlyLogs = logs.filter { log -> log.hourOfDay() == hour }

        return SustainabilityScore(
            date = date,
            hour = hour,
            actualScore = scoreCalculator.calculateActualScore(hourlyLogs),
            recommendedScore = scoreCalculator.calculateRecommendedScore(hourlyLogs)
        )
    }

    private fun ActivityLog.hourOfDay(): Int {
        return Calendar.getInstance()
            .apply { timeInMillis = timestampMillis }
            .get(Calendar.HOUR_OF_DAY)
    }
}
