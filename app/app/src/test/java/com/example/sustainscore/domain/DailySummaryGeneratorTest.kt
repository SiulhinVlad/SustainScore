package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.DailySummary
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DailySummaryGeneratorTest {

    private val scoreCalculator = ScoreCalculator(PredefinedActivities.all)
    private val hourlyScoreCalculator = HourlyScoreCalculator(scoreCalculator)
    private val generator = DailySummaryGenerator(
        activityTypes = PredefinedActivities.all,
        hourlyScoreCalculator = hourlyScoreCalculator
    )

    @Test
    fun emptyLogsReturnPerfectSummary() {
        val summary = generator.generate(
            date = "2026-05-18",
            logs = emptyList()
        )

        assertEquals(
            DailySummary(
                date = "2026-05-18",
                averageScore = 100.0,
                bestHour = null,
                worstHour = null,
                highestImpactActivityName = null
            ),
            summary
        )
    }

    @Test
    fun generatesAverageBestWorstAndHighestImpactActivity() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                timestampMillis = timestampForHour(8),
                amount = 2.0
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "computer_tv",
                timestampMillis = timestampForHour(9),
                amount = 2.0
            ),
            ActivityLog(
                id = "3",
                activityTypeId = "washing_machine",
                timestampMillis = timestampForHour(9),
                amount = 1.0
            )
        )

        val summary = generator.generate(
            date = "2026-05-18",
            logs = logs
        )

        assertEquals("2026-05-18", summary.date)
        assertEquals(88.0, summary.averageScore, 0.001)
        assertEquals(8, summary.bestHour)
        assertEquals(9, summary.worstHour)
        assertEquals("Washing Machine", summary.highestImpactActivityName)
    }

    @Test
    fun ignoresUnknownActivitiesWhenFindingHighestImpactActivity() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "unknown",
                timestampMillis = timestampForHour(8),
                amount = 100.0
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "lighting",
                timestampMillis = timestampForHour(8),
                amount = 1.0
            )
        )

        val summary = generator.generate(
            date = "2026-05-18",
            logs = logs
        )

        assertEquals("Lighting", summary.highestImpactActivityName)
    }

    @Test
    fun unknownOnlyActivitiesHaveNoHighestImpactActivity() {
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "unknown",
                timestampMillis = timestampForHour(8),
                amount = 100.0
            )
        )

        val summary = generator.generate(
            date = "2026-05-18",
            logs = logs
        )

        assertNull(summary.highestImpactActivityName)
    }

    private fun timestampForHour(hour: Int): Long {
        return LocalDateTime.of(2026, 5, 18, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
