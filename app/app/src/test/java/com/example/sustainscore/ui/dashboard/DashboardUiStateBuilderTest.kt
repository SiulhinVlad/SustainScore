package com.example.sustainscore.ui.dashboard

import com.example.sustainscore.model.ActivityLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.exp

class DashboardUiStateBuilderTest {

    @Test
    fun buildDashboardUiStateUsesExistingScoringSummaryAndWeeklyLogic() {
        val date = LocalDate.of(2026, 5, 18)
        val logs = listOf(
            ActivityLog(
                id = "1",
                activityTypeId = "lighting",
                startTimestampMillis = timestampForHour(date, 7),
                quantity = 3.0,
                durationMinutes = 60
            ),
            ActivityLog(
                id = "2",
                activityTypeId = "washing_machine",
                startTimestampMillis = timestampForHour(date, 13),
                quantity = 1.0,
                durationMinutes = 60
            ),
            ActivityLog(
                id = "3",
                activityTypeId = "computer_tv",
                startTimestampMillis = timestampForHour(date, 18),
                quantity = 2.0,
                durationMinutes = 60
            )
        )

        val state = buildDashboardUiState(date = date, logs = logs)

        val expectedCurrentScore = expectedScoreFromImpact(26.0)
        val expectedRecommendedScore = expectedScoreFromImpact(24.0)

        assertEquals(expectedCurrentScore, state.currentScore, 0.001)
        assertEquals(expectedRecommendedScore, state.recommendedScore, 0.001)
        assertEquals(3, state.logCount)
        assertEquals(3, state.trackedActivityCount)
        assertEquals(7, state.dailySummary.bestHour)
        assertEquals(13, state.dailySummary.worstHour)
        assertEquals("Washing Machine", state.dailySummary.highestImpactActivityName)
        assertTrue(state.dailyReport.contains("Washing Machine"))
        assertEquals(date.minusDays(6), state.weeklySummary.startDate)
        assertEquals(date, state.weeklySummary.endDate)
        assertEquals("Washing Machine", state.weeklySummary.highestImpactActivityName)
        assertEquals((expectedCurrentScore + 600.0) / 7.0, state.weeklySummary.averageScore, 0.001)
    }

    @Test
    fun buildDashboardUiStateHandlesEmptyLogs() {
        val date = LocalDate.of(2026, 5, 18)
        val state = buildDashboardUiState(
            date = date,
            logs = emptyList()
        )

        assertEquals(100.0, state.currentScore, 0.001)
        assertEquals(100.0, state.recommendedScore, 0.001)
        assertEquals(0, state.logCount)
        assertEquals(0, state.trackedActivityCount)
        assertNull(state.dailySummary.bestHour)
        assertNull(state.dailySummary.worstHour)
        assertNull(state.dailySummary.highestImpactActivityName)
        assertEquals(
            "No usage is logged yet. Add a few activities to receive a short sustainability report for today.",
            state.dailyReport
        )
        assertEquals(date.minusDays(6), state.weeklySummary.startDate)
        assertEquals(date, state.weeklySummary.endDate)
        assertNull(state.weeklySummary.highestImpactActivityName)
        assertEquals(100.0, state.weeklySummary.averageScore, 0.001)
    }

    @Test
    fun buildDashboardUiStateMatchesRecommendedScoreWhenUserIsAlreadyWithinLimits() {
        val date = LocalDate.of(2026, 5, 18)
        val state = buildDashboardUiState(
            date = date,
            logs = listOf(
                ActivityLog(
                    id = "1",
                    activityTypeId = "lighting",
                    startTimestampMillis = timestampForHour(date, 8),
                    quantity = 1.0,
                    durationMinutes = 60
                )
            )
        )

        assertEquals(expectedScoreFromImpact(2.0), state.currentScore, 0.001)
        assertEquals(expectedScoreFromImpact(2.0), state.recommendedScore, 0.001)
        assertTrue(state.dailyReport.contains("Lighting"))
    }

    @Test
    fun buildDashboardUiStateUsesAllLogsForWeeklySummaryEvenWhenSelectedDayHasDifferentFocus() {
        val date = LocalDate.of(2026, 5, 18)
        val selectedDateLogs = listOf(
            ActivityLog(
                id = "lighting-log",
                activityTypeId = "lighting",
                startTimestampMillis = timestampForHour(date, 8),
                quantity = 1.0,
                durationMinutes = 60
            )
        )
        val allLogs = selectedDateLogs + ActivityLog(
            id = "washing-log",
            activityTypeId = "washing_machine",
            startTimestampMillis = timestampForHour(date.minusDays(1), 9),
            quantity = 1.0,
            durationMinutes = 60
        )

        val state = buildDashboardUiState(
            date = date,
            logs = selectedDateLogs,
            allLogs = allLogs
        )

        assertEquals("Washing Machine", state.weeklySummary.highestImpactActivityName)
        assertEquals(
            (500.0 + expectedScoreFromImpact(2.0) + expectedScoreFromImpact(12.0)) / 7.0,
            state.weeklySummary.averageScore,
            0.001
        )
    }

    private fun timestampForHour(date: LocalDate, hour: Int): Long {
        return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun expectedScoreFromImpact(impact: Double): Double {
        return 100.0 * exp(-impact / 100.0)
    }
}
