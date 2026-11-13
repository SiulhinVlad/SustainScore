package com.example.sustainscore

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnergySharingDemoTest {
    @get:Rule(order = 0)
    val cleanDemo = object : ExternalResource() {
        override fun before() {
            InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences("sustainshare_demo", 0).edit().clear().commit()
        }
    }

    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun calculatesChartsUpdatesFromNewUsageAndSurvivesRecreation() {
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("energy_prediction_card"))
        compose.onNodeWithTag("user_prediction_chart").assertDoesNotExist()
        compose.onNodeWithTag("neighbours_prediction_chart").assertDoesNotExist()
        compose.onNodeWithTag("calculate_prediction_button").assertIsDisplayed().performClick()
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("user_prediction_chart"))
        compose.onNodeWithTag("user_prediction_chart").assertIsDisplayed()
        val before = compose.onNodeWithTag("user_prediction_chart").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].joinToString()
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("neighbours_prediction_chart"))
        compose.onNodeWithTag("neighbours_prediction_chart").assertIsDisplayed().performTouchInput { click(centerRight) }
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("update_prediction_button"))
        compose.onNodeWithTag("update_prediction_button").assertIsDisplayed().performClick()
        compose.onNodeWithTag("calculate_prediction_button").assertDoesNotExist()

        compose.onNodeWithTag("dashboard_log_activity_button").performClick()
        compose.onNodeWithTag("log_activity_quantity_field").performTextInput("10")
        compose.onNodeWithTag("log_activity_duration_field").performTextInput("02:00")
        compose.onNodeWithTag("log_activity_start_time_field").performTextInput("07:00")
        compose.onNodeWithTag("log_activity_list").performScrollToNode(hasText("Save"))
        compose.onNodeWithTag("log_activity_save_button").performClick()
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("user_prediction_chart"))
        val after = compose.onNodeWithTag("user_prediction_chart").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].joinToString()
        assertNotEquals(before, after)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("user_prediction_chart"))
        val restored = compose.onNodeWithTag("user_prediction_chart").fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].joinToString()
        assertTrue(restored == after)
        compose.onNodeWithTag("dashboard_list").performScrollToNode(hasText("AI Generated Suggestions"))
        compose.onNodeWithText("AI Generated Suggestions").assertIsDisplayed()
    }
}
