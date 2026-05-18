package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.exp

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
                startTimestampMillis = timestampForHour(9),
                quantity = 3.0,
                durationMinutes = 60
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "computer_tv",
                startTimestampMillis = timestampForHour(10),
                quantity = 4.0,
                durationMinutes = 60
            )
        )

        val score = calculator.calculate(
            date = "2026-05-17",
            hour = 9,
            logs = logs
        )

        assertEquals("2026-05-17", score.date)
        assertEquals(9, score.hour)
        assertEquals(expectedScoreFromImpact(6.0), score.actualScore, 0.001)
        assertEquals(expectedScoreFromImpact(4.0), score.recommendedScore, 0.001)
    }

    private fun timestampForHour(hour: Int): Long {
        return LocalDateTime.of(2026, 5, 17, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun expectedScoreFromImpact(impact: Double): Double {
        return 100.0 * exp(-impact / 100.0)
    }
}
