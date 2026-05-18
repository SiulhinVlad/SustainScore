package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Test

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
                timestampMillis = 0L,
                amount = 3.0
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "computer_tv",
                timestampMillis = 0L,
                amount = 2.0
            )
        )

        val score = calculator.calculateActualScore(logs)

        assertEquals(86.0, score, 0.001)
    }

    @Test
    fun scoreNeverGoesBelowZero() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "washing_machine",
                timestampMillis = 0L,
                amount = 20.0
            )
        )

        val score = calculator.calculateActualScore(logs)

        assertEquals(0.0, score, 0.001)
    }

    @Test
    fun unknownActivityTypeDoesNotAffectScore() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "unknown",
                timestampMillis = 0L,
                amount = 10.0
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
                timestampMillis = 0L,
                amount = 5.0
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "computer_tv",
                timestampMillis = 0L,
                amount = 6.0
            )
        )

        val score = calculator.calculateRecommendedScore(logs)

        assertEquals(88.0, score, 0.001)
    }

    @Test
    fun recommendedScoreCountsEachActivityTypeOnce() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                timestampMillis = 0L,
                amount = 5.0
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "lighting",
                timestampMillis = 0L,
                amount = 7.0
            )
        )

        val score = calculator.calculateRecommendedScore(logs)

        assertEquals(96.0, score, 0.001)
    }
}
