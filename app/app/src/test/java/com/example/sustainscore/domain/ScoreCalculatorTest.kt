package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class ScoreCalculatorTest {

    private val calculator = ScoreCalculator(PredefinedActivities.all)

    @Test
    fun emptyActivityListReturnsPerfectScore() {
        val score = calculator.calculateActualScore(emptyList())

        assertEquals(100.0, score, 0.001)
    }

    @Test
    fun loggedActivitiesReduceScoreByWeightedConsumption() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                startTimestampMillis = 0L,
                quantity = 3.0,
                durationMinutes = 60
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "computer_tv",
                startTimestampMillis = 0L,
                quantity = 2.0,
                durationMinutes = 60
            )
        )

        val score = calculator.calculateActualScore(logs)

        assertEquals(expectedScoreFromImpact(14.0), score, 0.001)
    }

    @Test
    fun worseConsumptionContinuesToLowerScoreInsteadOfFlatliningAtZero() {
        val mediumConsumptionLogs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "washing_machine",
                startTimestampMillis = 0L,
                quantity = 10.0,
                durationMinutes = 60
            )
        )
        val highConsumptionLogs = listOf(
            ActivityLog(
                id = "2",
                activityTypeId = "washing_machine",
                startTimestampMillis = 0L,
                quantity = 20.0,
                durationMinutes = 60
            )
        )

        val mediumScore = calculator.calculateActualScore(mediumConsumptionLogs)
        val highScore = calculator.calculateActualScore(highConsumptionLogs)

        assertTrue(mediumScore > 0.0)
        assertTrue(highScore > 0.0)
        assertTrue(highScore < mediumScore)
    }

    @Test
    fun unknownActivityTypeDoesNotAffectScore() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "unknown",
                startTimestampMillis = 0L,
                quantity = 10.0,
                durationMinutes = 60
            )
        )

        val score = calculator.calculateActualScore(logs)

        assertEquals(100.0, score, 0.001)
    }

    @Test
    fun recommendedScoreUsesRecommendedLimitsForLoggedActivityTypes() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                startTimestampMillis = 0L,
                quantity = 5.0,
                durationMinutes = 60
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "computer_tv",
                startTimestampMillis = 0L,
                quantity = 6.0,
                durationMinutes = 60
            )
        )

        val score = calculator.calculateRecommendedScore(logs)

        assertEquals(expectedScoreFromImpact(12.0), score, 0.001)
    }

    @Test
    fun recommendedScoreMatchesCurrentScoreWhenUsageIsAlreadyWithinLimit() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                startTimestampMillis = 0L,
                quantity = 1.0,
                durationMinutes = 60
            )
        )

        val currentScore = calculator.calculateActualScore(logs)
        val recommendedScore = calculator.calculateRecommendedScore(logs)

        assertEquals(currentScore, recommendedScore, 0.001)
    }

    @Test
    fun recommendedScoreImprovesWhenUsageExceedsLimit() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                startTimestampMillis = 0L,
                quantity = 5.0,
                durationMinutes = 60
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "lighting",
                startTimestampMillis = 0L,
                quantity = 7.0,
                durationMinutes = 60
            )
        )

        val currentScore = calculator.calculateActualScore(logs)
        val recommendedScore = calculator.calculateRecommendedScore(logs)

        assertTrue(recommendedScore > currentScore)
        assertEquals(expectedScoreFromImpact(4.0 + 4.0), recommendedScore, 0.001)
    }

    private fun expectedScoreFromImpact(impact: Double): Double {
        return 100.0 * exp(-impact / 100.0)
    }
}
