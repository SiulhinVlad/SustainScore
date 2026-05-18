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
                averageScore = 100.0,
                bestHour = null,
                worstHour = null,
                highestImpactActivityName = null
            )
        }

        val activeHours = logs.map { it.hourOfDay() }.distinct().sorted()
        val hourlyScores = activeHours.map { hour ->
            hourlyScoreCalculator.calculate(date, hour, logs)
        }

        val bestScore = hourlyScores.maxBy { it.actualScore }
        val worstScore = hourlyScores.minBy { it.actualScore }
        val averageScore = hourlyScores.map { it.actualScore }.average()
        val highestImpactActivity = logs
            .mapNotNull { log ->
                val activityType = activityTypeById[log.activityTypeId] ?: return@mapNotNull null
                activityType to log.amount * activityType.weight
            }
            .maxByOrNull { it.second }
            ?.first

        return DailySummary(
            date = date,
            averageScore = averageScore,
            bestHour = bestScore.hour,
            worstHour = worstScore.hour,
            highestImpactActivityName = highestImpactActivity?.name
        )
    }

    private fun ActivityLog.hourOfDay(): Int {
        return Calendar.getInstance()
            .apply { timeInMillis = timestampMillis }
            .get(Calendar.HOUR_OF_DAY)
    }
}
