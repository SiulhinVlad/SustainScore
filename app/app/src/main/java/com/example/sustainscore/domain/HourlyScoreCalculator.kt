package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.SustainabilityScore
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

class HourlyScoreCalculator(
    private val scoreCalculator: ScoreCalculator
) {
    fun calculate(date: String, hour: Int, logs: List<ActivityLog>): SustainabilityScore {
        val hourlyLogs = logs.mapNotNull { log -> log.portionInsideHour(hour) }

        return SustainabilityScore(
            date = date,
            hour = hour,
            actualScore = scoreCalculator.calculateActualScore(hourlyLogs),
            recommendedScore = scoreCalculator.calculateRecommendedScore(hourlyLogs)
        )
    }

    private fun ActivityLog.portionInsideHour(hour: Int): ActivityLog? {
        val hourStart = hourStartMillis(startTimestampMillis, hour)
        val hourEnd = hourStart + ONE_HOUR_MILLIS
        val logStart = startTimestampMillis
        val logEnd = startTimestampMillis + durationMinutes * ONE_MINUTE_MILLIS
        val overlapStart = max(logStart, hourStart)
        val overlapEnd = min(logEnd, hourEnd)
        val overlapMinutes = ((overlapEnd - overlapStart) / ONE_MINUTE_MILLIS).toInt()

        if (overlapMinutes <= 0) {
            return null
        }

        return copy(durationMinutes = overlapMinutes)
    }

    private fun hourStartMillis(referenceTimestampMillis: Long, hour: Int): Long {
        return Calendar.getInstance()
            .apply {
                timeInMillis = referenceTimestampMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis
    }

    companion object {
        private const val ONE_MINUTE_MILLIS = 60_000L
        private const val ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS
    }
}
