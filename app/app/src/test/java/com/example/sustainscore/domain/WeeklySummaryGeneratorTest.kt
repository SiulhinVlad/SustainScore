package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.exp

class WeeklySummaryGeneratorTest {

    private val scoreCalculator = ScoreCalculator(PredefinedActivities.all)
    private val generator = WeeklySummaryGenerator(
        activityTypes = PredefinedActivities.all,
        scoreCalculator = scoreCalculator
    )

    @Test
    fun generateBuildsSevenDayWindowAverageAndHighestImpactActivity() {
        val endDate = LocalDate.of(2026, 5, 18)
        val summary = generator.generate(
            endDate = endDate,
            logs = listOf(
                ActivityLog(
                    id = "outside-window",
                    activityTypeId = "heating_cooling",
                    startTimestampMillis = timestampFor(endDate.minusDays(8), 10),
                    quantity = 4.0,
                    durationMinutes = 60
                ),
                ActivityLog(
                    id = "washing",
                    activityTypeId = "washing_machine",
                    startTimestampMillis = timestampFor(endDate.minusDays(1), 9),
                    quantity = 1.0,
                    durationMinutes = 60
                ),
                ActivityLog(
                    id = "lights",
                    activityTypeId = "lighting",
                    startTimestampMillis = timestampFor(endDate, 8),
                    quantity = 1.0,
                    durationMinutes = 60
                )
            )
        )

        assertEquals(endDate.minusDays(6), summary.startDate)
        assertEquals(endDate, summary.endDate)
        assertEquals("Washing Machine", summary.highestImpactActivityName)
        assertEquals(
            (500.0 + expectedScoreFromImpact(12.0) + expectedScoreFromImpact(2.0)) / 7.0,
            summary.averageScore,
            0.001
        )
    }

    @Test
    fun generateReturnsPerfectAverageWhenWeekHasNoLogs() {
        val endDate = LocalDate.of(2026, 5, 18)
        val summary = generator.generate(
            endDate = endDate,
            logs = emptyList()
        )

        assertEquals(endDate.minusDays(6), summary.startDate)
        assertEquals(endDate, summary.endDate)
        assertNull(summary.highestImpactActivityName)
        assertEquals(100.0, summary.averageScore, 0.001)
    }

    private fun timestampFor(date: LocalDate, hour: Int): Long {
        return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun expectedScoreFromImpact(impact: Double): Double {
        return 100.0 * exp(-impact / 100.0)
    }
}
