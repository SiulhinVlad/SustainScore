package com.example.sustainscore

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun logActivityButtonNavigatesToLogActivityScreenAndBackReturnsToDashboard() {
        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasText("Log Activity"))

        composeRule.onNodeWithTag("dashboard_log_activity_button")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithTag("log_activity_list")
            .assertIsDisplayed()

        composeRule.onNodeWithTag("log_activity_list")
            .performScrollToNode(hasText("Back"))

        composeRule.onNodeWithTag("log_activity_back_button")
            .assertIsDisplayed()
            .performClick()

        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasText("Daily summary"))

        composeRule.onNodeWithText("Daily summary")
            .assertIsDisplayed()
    }

    @Test
    fun savingActivityReturnsToDashboardAndUpdatesCurrentScore() {
        composeRule.onNodeWithTag("dashboard_list")
            .performScrollToNode(hasText("Log Activity"))

        composeRule.onNodeWithTag("dashboard_log_activity_button")
            .performClick()

        composeRule.onNodeWithTag("log_activity_quantity_field")
            .performTextInput("1")

        composeRule.onNodeWithTag("log_activity_duration_field")
            .performTextInput("01:00")

        composeRule.onNodeWithTag("log_activity_start_time_field")
            .performTextInput("20:00")

        composeRule.onNodeWithTag("log_activity_list")
            .performScrollToNode(hasText("Save"))

        composeRule.onNodeWithTag("log_activity_save_button")
            .performClick()

        composeRule.onNodeWithText("Dashboard")
            .assertIsDisplayed()

        composeRule.onNodeWithText("76 / 100")
            .assertIsDisplayed()
    }
}
