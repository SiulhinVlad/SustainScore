package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import kotlin.math.exp

class ScoreCalculator(
    private val activityTypes: List<ActivityType>
) {
    private val activityTypeById = activityTypes.associateBy { it.id }

    fun calculateActualScore(logs: List<ActivityLog>): Double {
        val totalWeightedConsumption = calculateActualImpact(logs)

        return scoreFromWeightedConsumption(totalWeightedConsumption)
    }

    fun calculateRecommendedScore(logs: List<ActivityLog>): Double {
        val totalRecommendedConsumption = calculateRecommendedImpact(logs)

        return scoreFromWeightedConsumption(totalRecommendedConsumption)
    }

    fun calculate(logs: List<ActivityLog>): Double = calculateActualScore(logs)

    internal fun calculateActualImpact(logs: List<ActivityLog>): Double {
        return logs.sumOf { log ->
            val activityType = activityTypeById[log.activityTypeId]
            if (activityType == null) {
                0.0
            } else {
                log.quantityHours * activityType.weight
            }
        }
    }

    internal fun calculateRecommendedImpact(logs: List<ActivityLog>): Double {
        return logs.sumOf { log ->
            val activityType = activityTypeById[log.activityTypeId]
            if (activityType == null) {
                0.0
            } else {
                val sustainableQuantity = minOf(log.quantity, activityType.recommendedLimit)
                sustainableQuantity * log.durationHours * activityType.weight
            }
        }
    }

    private fun scoreFromWeightedConsumption(weightedConsumption: Double): Double {
        return 100.0 * exp(-weightedConsumption / ScoreScale)
    }

    private companion object {
        private const val ScoreScale = 100.0
    }
}
