package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class HourlyScoreCalculatorTest {

    private val calculator = HourlyScoreCalculator(
        ScoreCalculator(PredefinedActivities.all)
    )

    @Test
    fun calculatesActualAndRecommendedScoreForRequestedHour() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                timestampMillis = timestampForHour(9),
                amount = 3.0
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "computer_tv",
                timestampMillis = timestampForHour(10),
                amount = 4.0
            )
        )

        val score = calculator.calculate(
            date = "2026-05-17",
            hour = 9,
            logs = logs
        )

        assertEquals("2026-05-17", score.date)
        assertEquals(9, score.hour)
        assertEquals(94.0, score.actualScore, 0.001)
        assertEquals(96.0, score.recommendedScore, 0.001)
    }

    private fun timestampForHour(hour: Int): Long {
        return LocalDateTime.of(2026, 5, 17, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
