package com.example.sustainscore.domain

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.DailySummary
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DailyAiReportGeneratorTest {

    private val generator = DailyAiReportGenerator(PredefinedActivities.all)

    @Test
    fun generateReturnsEmptyStateReportWhenNoLogsExist() {
        val report = generator.generate(
            logs = emptyList(),
            currentScore = 100.0,
            recommendedScore = 100.0,
            dailySummary = DailySummary(
                date = "2026-05-18",
                bestHour = null,
                worstHour = null,
                highestImpactActivityName = null
            )
        )

        assertEquals(
            "No usage is logged yet. Add a few activities to receive a short sustainability report for today.",
            report
        )
    }

    @Test
    fun generateHighlightsHighestImpactActivityWhenBelowTarget() {
        val report = generator.generate(
            logs = listOf(
                ActivityLog(
                    id = "washing",
                    activityTypeId = "washing_machine",
                    startTimestampMillis = timestampForHour(13),
                    quantity = 2.0,
                    durationMinutes = 60
                )
            ),
            currentScore = 55.0,
            recommendedScore = 72.0,
            dailySummary = DailySummary(
                date = "2026-05-18",
                bestHour = 8,
                worstHour = 13,
                highestImpactActivityName = "Washing Machine"
            )
        )

        assertTrue(report.contains("bad range"))
        assertTrue(report.contains("Washing Machine"))
        assertTrue(report.contains("72"))
    }

    @Test
    fun generateEncouragesUserWhenAlreadyMeetingTarget() {
        val report = generator.generate(
            logs = listOf(
                ActivityLog(
                    id = "lights",
                    activityTypeId = "lighting",
                    startTimestampMillis = timestampForHour(7),
                    quantity = 1.0,
                    durationMinutes = 60
                )
            ),
            currentScore = 89.0,
            recommendedScore = 89.0,
            dailySummary = DailySummary(
                date = "2026-05-18",
                bestHour = 7,
                worstHour = 7,
                highestImpactActivityName = "Lighting"
            )
        )

        assertTrue(report.contains("good range"))
        assertTrue(report.contains("Lighting"))
        assertTrue(report.contains("sustainable target"))
    }

    private fun timestampForHour(hour: Int): Long {
        return LocalDateTime.of(2026, 5, 18, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
