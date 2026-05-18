package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType

class ScoreCalculator(
    private val activityTypes: List<ActivityType>
) {
    private val activityTypeById = activityTypes.associateBy { it.id }

    fun calculateActualScore(logs: List<ActivityLog>): Double {
        val totalWeightedConsumption = logs.sumOf { log ->
            val activityType = activityTypeById[log.activityTypeId]
            if (activityType == null) {
                0.0
            } else {
                log.quantityHours * activityType.weight
            }
        }

        return scoreFromWeightedConsumption(totalWeightedConsumption)
    }

    fun calculateRecommendedScore(logs: List<ActivityLog>): Double {
        val totalRecommendedConsumption = logs
            .mapNotNull { log -> activityTypeById[log.activityTypeId] }
            .distinctBy { it.id }
            .sumOf { activityType -> activityType.recommendedLimit * activityType.weight }

        return scoreFromWeightedConsumption(totalRecommendedConsumption)
    }

    fun calculate(logs: List<ActivityLog>): Double = calculateActualScore(logs)

    private fun scoreFromWeightedConsumption(weightedConsumption: Double): Double {
        return (100.0 - weightedConsumption).coerceIn(0.0, 100.0)
    }
}
