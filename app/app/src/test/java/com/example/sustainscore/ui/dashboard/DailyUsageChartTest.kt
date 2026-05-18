package com.example.sustainscore.ui.dashboard

import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.PredefinedActivities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DailyUsageChartTest {

    @Test
    fun logsForDateReturnsOnlyLogsFromSelectedDay() {
        val selectedDate = LocalDate.of(2026, 5, 18)
        val logs = listOf(
            ActivityLog(
                id = "selected-day",
                activityTypeId = "lighting",
                startTimestampMillis = timestampFor(selectedDate, 8, 0),
                quantity = 1.0,
                durationMinutes = 60
            ),
            ActivityLog(
                id = "other-day",
                activityTypeId = "washing_machine",
                startTimestampMillis = timestampFor(selectedDate.plusDays(1), 8, 0),
                quantity = 1.0,
                durationMinutes = 60
            )
        )

        val filteredLogs = logsForDate(logs, selectedDate)

        assertEquals(listOf("selected-day"), filteredLogs.map { it.id })
    }

    @Test
    fun buildDailyUsageChartLayoutSplitsBlocksWhenLowerSpaceOpensUp() {
        val date = LocalDate.of(2026, 5, 18)
        val layout = buildDailyUsageChartLayout(
            date = date,
            logs = listOf(
                ActivityLog(
                    id = "lights",
                    activityTypeId = "lighting",
                    startTimestampMillis = timestampFor(date, 2, 0),
                    quantity = 2.0,
                    durationMinutes = 120
                ),
                ActivityLog(
                    id = "wash",
                    activityTypeId = "washing_machine",
                    startTimestampMillis = timestampFor(date, 3, 0),
                    quantity = 1.0,
                    durationMinutes = 120
                )
            ),
            activityTypes = PredefinedActivities.all
        )

        assertEquals(3, layout.blocks.size)

        val lightsBlock = layout.blocks.first { it.id == "lights" }
        val washingOverlapSegment = layout.blocks.first {
            it.id == "wash" && it.startMinute == 180
        }
        val washingFallenSegment = layout.blocks.first {
            it.id == "wash" && it.startMinute == 240
        }

        assertEquals(120, lightsBlock.startMinute)
        assertEquals(240, lightsBlock.endMinute)
        assertEquals(4.0, lightsBlock.impact, 0.001)
        assertEquals(0.0, lightsBlock.stackBase, 0.001)

        assertEquals(180, washingOverlapSegment.startMinute)
        assertEquals(240, washingOverlapSegment.endMinute)
        assertEquals(12.0, washingOverlapSegment.impact, 0.001)
        assertEquals(4.0, washingOverlapSegment.stackBase, 0.001)

        assertEquals(240, washingFallenSegment.startMinute)
        assertEquals(300, washingFallenSegment.endMinute)
        assertEquals(12.0, washingFallenSegment.impact, 0.001)
        assertEquals(0.0, washingFallenSegment.stackBase, 0.001)

        assertEquals(16.0, layout.maxDepth, 0.001)
    }

    @Test
    fun buildDailyUsageChartLayoutBuildsHourlyDepthLineFromStackBottoms() {
        val date = LocalDate.of(2026, 5, 18)
        val layout = buildDailyUsageChartLayout(
            date = date,
            logs = listOf(
                ActivityLog(
                    id = "lights",
                    activityTypeId = "lighting",
                    startTimestampMillis = timestampFor(date, 2, 0),
                    quantity = 2.0,
                    durationMinutes = 120
                ),
                ActivityLog(
                    id = "wash",
                    activityTypeId = "washing_machine",
                    startTimestampMillis = timestampFor(date, 3, 0),
                    quantity = 1.0,
                    durationMinutes = 120
                )
            ),
            activityTypes = PredefinedActivities.all
        )

        val depthByHour = layout.hourDepths.associate { it.hour to it.depth }

        assertEquals(0.0, depthByHour[1] ?: -1.0, 0.001)
        assertEquals(4.0, depthByHour[2] ?: -1.0, 0.001)
        assertEquals(16.0, depthByHour[3] ?: -1.0, 0.001)
        assertEquals(12.0, depthByHour[4] ?: -1.0, 0.001)
        assertEquals(0.0, depthByHour[5] ?: -1.0, 0.001)
        assertTrue(layout.hourDepths.any { it.hour == 24 && it.depth == 0.0 })
    }

    private fun timestampFor(date: LocalDate, hour: Int, minute: Int): Long {
        return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
