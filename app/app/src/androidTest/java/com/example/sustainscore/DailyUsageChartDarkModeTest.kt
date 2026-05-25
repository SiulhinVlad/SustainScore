package com.example.sustainscore

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sustainscore.model.ActivityLog
import com.example.sustainscore.model.PredefinedActivities
import com.example.sustainscore.ui.dashboard.DailyUsageChartCard
import com.example.sustainscore.ui.theme.SustainScoreTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class DailyUsageChartDarkModeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dailyUsageChartShowsActualAndRecommendedGraphsInDarkMode() {
        val date = LocalDate.of(2026, 5, 18)
        val logs = listOf(
            ActivityLog(
                id = "washing-machine",
                activityTypeId = "washing_machine",
                startTimestampMillis = timestampFor(date, 7, 0),
                quantity = 1.0,
                durationMinutes = 180
            ),
            ActivityLog(
                id = "lights",
                activityTypeId = "lighting",
                startTimestampMillis = timestampFor(date, 7, 0),
                quantity = 4.0,
                durationMinutes = 120
            ),
            ActivityLog(
                id = "computer",
                activityTypeId = "computer_tv",
                startTimestampMillis = timestampFor(date, 8, 0),
                quantity = 3.0,
                durationMinutes = 180
            )
        )

        composeRule.setContent {
            SustainScoreTheme(darkTheme = true) {
                DailyUsageChartCard(
                    anchorDate = date,
                    selectedDate = date,
                    logs = logs,
                    activityTypes = PredefinedActivities.all,
                    onSelectedDateChange = {}
                )
            }
        }

        composeRule.onNodeWithTag("daily_usage_chart_canvas_selected")
            .assertIsDisplayed()

        val pixelMap = composeRule.onNodeWithTag("daily_usage_chart_canvas_selected")
            .captureToImage()
            .toPixelMap()

        assertTrue(hasColorfulUsagePixels(pixelMap))
        assertTrue(hasRecommendedGrayPixels(pixelMap))
    }

    private fun hasColorfulUsagePixels(pixelMap: androidx.compose.ui.graphics.PixelMap): Boolean {
        return sampledPixels(pixelMap).any { color ->
            color.alpha > 0.1f &&
                (
                    abs(color.red - color.green) > 0.12f ||
                        abs(color.green - color.blue) > 0.12f ||
                        abs(color.red - color.blue) > 0.12f
                    )
        }
    }

    private fun hasRecommendedGrayPixels(pixelMap: androidx.compose.ui.graphics.PixelMap): Boolean {
        val startY = pixelMap.height / 5
        return sampledPixels(pixelMap, startY = startY).any { color ->
            color.alpha > 0.1f &&
                colorBrightness(color) in 0.35f..0.85f &&
                abs(color.red - color.green) < 0.08f &&
                abs(color.green - color.blue) < 0.08f
        }
    }

    private fun sampledPixels(
        pixelMap: androidx.compose.ui.graphics.PixelMap,
        startY: Int = 0
    ): Sequence<Color> = sequence {
        val stepX = maxOf(1, pixelMap.width / 80)
        val stepY = maxOf(1, pixelMap.height / 80)
        for (y in startY until pixelMap.height step stepY) {
            for (x in 0 until pixelMap.width step stepX) {
                yield(pixelMap[x, y])
            }
        }
    }

    private fun colorBrightness(color: Color): Float {
        return (color.red + color.green + color.blue) / 3f
    }

    private fun timestampFor(date: LocalDate, hour: Int, minute: Int): Long {
        return LocalDateTime.of(date.year, date.monthValue, date.dayOfMonth, hour, minute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
