package com.example.sustainscore.ui.dashboard

import com.example.sustainscore.model.ActivityLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DashboardUiStateBuilderTest {

    @Test
    fun buildDashboardUiStateUsesExistingScoringAndSummaryLogic() {
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

        val state = buildDashboardUiState(date, logs)

        assertEquals(74.0, state.currentScore, 0.001)
        assertEquals(76.0, state.recommendedScore, 0.001)
        assertEquals(3, state.logCount)
        assertEquals(3, state.trackedActivityCount)
        assertEquals(91.333, state.dailySummary.averageScore, 0.001)
        assertEquals(7, state.dailySummary.bestHour)
        assertEquals(13, state.dailySummary.worstHour)
        assertEquals("Washing Machine", state.dailySummary.highestImpactActivityName)
    }

    @Test
    fun buildDashboardUiStateHandlesEmptyLogs() {
        val state = buildDashboardUiState(
            date = LocalDate.of(2026, 5, 18),
            logs = emptyList()
        )

        assertEquals(100.0, state.currentScore, 0.001)
        assertEquals(100.0, state.recommendedScore, 0.001)
        assertEquals(0, state.logCount)
        assertEquals(0, state.trackedActivityCount)
        assertEquals(100.0, state.dailySummary.averageScore, 0.001)
        assertNull(state.dailySummary.bestHour)
        assertNull(state.dailySummary.worstHour)
        assertNull(state.dailySummary.highestImpactActivityName)
    }

    @Test
    fun buildRecommendationTextPromptsForFirstLogWhenNoActivitiesExist() {
        val state = buildDashboardUiState(
            date = LocalDate.of(2026, 5, 18),
            logs = emptyList()
        )

        val recommendation = buildRecommendationText(state)

        assertEquals(
            "Add your first activity to receive a sustainability suggestion.",
            recommendation
        )
    }

    @Test
    fun buildRecommendationTextHighlightsHighestImpactActivityWhenBelowTarget() {
        val state = buildDashboardUiState(
            date = LocalDate.of(2026, 5, 18),
            logs = listOf(
                ActivityLog(
                    id = "1",
                    activityTypeId = "washing_machine",
                    startTimestampMillis = timestampForHour(LocalDate.of(2026, 5, 18), 13),
                    quantity = 2.0,
                    durationMinutes = 60
                )
            )
        )

        val recommendation = buildRecommendationText(state)

        assertEquals(
            "Focusing on Washing Machine is the clearest way to move closer to today's recommended score.",
            recommendation
        )
    }

    @Test
    fun buildRecommendationTextEncouragesUserWhenMeetingRecommendedScore() {
        val state = buildDashboardUiState(
            date = LocalDate.of(2026, 5, 18),
            logs = listOf(
                ActivityLog(
                    id = "1",
                    activityTypeId = "lighting",
                    startTimestampMillis = timestampForHour(LocalDate.of(2026, 5, 18), 8),
                    quantity = 1.0,
                    durationMinutes = 60
                )
            )
        )

        val recommendation = buildRecommendationText(state)

        assertEquals(
            "You are already meeting or beating the recommended score for today's activity mix. Keep the same pattern going.",
            recommendation
        )
    }

    @Test
    fun buildRecommendationTextFallsBackWhenHighestImpactActivityIsUnknown() {
        val state = DashboardUiState(
            displayDate = "Monday, 18 May",
            currentScore = 70.0,
            recommendedScore = 85.0,
            dailySummary = com.example.sustainscore.model.DailySummary(
                date = "2026-05-18",
                averageScore = 70.0,
                bestHour = 8,
                worstHour = 20,
                highestImpactActivityName = null
            ),
            logCount = 1,
            trackedActivityCount = 1
        )

        val recommendation = buildRecommendationText(state)

        assertEquals(
            "Reducing your highest-use activity today would move you closer to the recommended score.",
            recommendation
        )
    }

    private fun timestampForHour(date: LocalDate, hour: Int): Long {
        return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
