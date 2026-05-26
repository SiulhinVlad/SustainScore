package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.ActivityType
import com.example.sustainscore.model.DailySummary
import kotlin.math.roundToInt

class DailyAiReportGenerator(
    activityTypes: List<ActivityType>
) {
    private val activityTypeById = activityTypes.associateBy { it.id }

    fun generate(
        logs: List<ActivityLog>,
        currentScore: Double,
        recommendedScore: Double,
        dailySummary: DailySummary
    ): String {
        if (logs.isEmpty()) {
            return "No usage is logged yet. Add a few activities to receive a short sustainability report for today."
        }

        val topActivities = logs
            .mapNotNull { log ->
                val activityType = activityTypeById[log.activityTypeId] ?: return@mapNotNull null
                activityType.name to log.quantityHours * activityType.weight
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, impacts) -> impacts.sum() }
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

        val usagePhrase = when (topActivities.size) {
            0 -> "usage pattern"
            1 -> topActivities.first()
            else -> "${topActivities[0]} and ${topActivities[1]}"
        }
        val focusActivity = dailySummary.highestImpactActivityName ?: topActivities.firstOrNull() ?: "your largest-use activity"
        val secondaryActivity = topActivities.firstOrNull { it != focusActivity }
        val scoreRangeLabel = scoreRangeLabel(currentScore)
        val roundedRecommendedScore = recommendedScore.roundToInt()

        return if (currentScore >= recommendedScore) {
            "Today's score sits in the $scoreRangeLabel range. Your $usagePhrase use is already close to the sustainable target, so keep $focusActivity steady and avoid adding extra usage later today."
        } else {
            val followUp = if (secondaryActivity != null) {
                "Then keep $secondaryActivity lighter as well"
            } else {
                "Then keep the rest of today's usage lighter"
            }
            "Today's score sits in the $scoreRangeLabel range. Most of the impact came from $focusActivity, so reduce that first. $followUp to move closer to $roundedRecommendedScore."
        }
    }

    private fun scoreRangeLabel(score: Double): String {
        return when {
            score >= 80.0 -> "good"
            score >= 60.0 -> "okay"
            score >= 40.0 -> "bad"
            else -> "danger"
        }
    }
}
