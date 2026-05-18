package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import com.example.sustainscore.model.DailySummary
import java.util.Calendar

class DailySummaryGenerator(
    activityTypes: List<ActivityType>,
    private val hourlyScoreCalculator: HourlyScoreCalculator
) {
    private val activityTypeById = activityTypes.associateBy { it.id }

    fun generate(date: String, logs: List<ActivityLog>): DailySummary {
        if (logs.isEmpty()) {
            return DailySummary(
                date = date,
                bestHour = null,
                worstHour = null,
                highestImpactActivityName = null
            )
        }

        val activeHours = logs.flatMap { it.activeHours() }.distinct().sorted()
        val hourlyScores = activeHours.map { hour ->
            hourlyScoreCalculator.calculate(date, hour, logs)
        }

        val bestScore = hourlyScores.maxBy { it.actualScore }
        val worstScore = hourlyScores.minBy { it.actualScore }
        val highestImpactActivity = logs
            .mapNotNull { log ->
                val activityType = activityTypeById[log.activityTypeId] ?: return@mapNotNull null
                activityType to log.quantityHours * activityType.weight
            }
            .maxByOrNull { it.second }
            ?.first

        return DailySummary(
            date = date,
            bestHour = bestScore.hour,
            worstHour = worstScore.hour,
            highestImpactActivityName = highestImpactActivity?.name
        )
    }

    private fun ActivityLog.activeHours(): List<Int> {
        val activeHours = mutableListOf<Int>()
        val logEndMillis = startTimestampMillis + durationMinutes * ONE_MINUTE_MILLIS
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startTimestampMillis
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        while (calendar.timeInMillis < logEndMillis) {
            activeHours.add(calendar.get(Calendar.HOUR_OF_DAY))
            calendar.add(Calendar.HOUR_OF_DAY, 1)
        }

        return activeHours
    }

    companion object {
        private const val ONE_MINUTE_MILLIS = 60_000L
    }
}
