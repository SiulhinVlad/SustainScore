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

        assertEquals(86.0, score, 0.001)
    }

    @Test
    fun scoreNeverGoesBelowZero() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "washing_machine",
                startTimestampMillis = 0L,
                quantity = 20.0,
                durationMinutes = 60
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

        assertEquals(88.0, score, 0.001)
    }

    @Test
    fun recommendedScoreCountsEachActivityTypeOnce() {
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

        val score = calculator.calculateRecommendedScore(logs)

        assertEquals(96.0, score, 0.001)
    }
}
