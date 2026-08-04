package io.github.arfaouiahmed1.pursivo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class PursivoSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onboardingReachesTodayAndQuickAdd() {
        if (composeRule.onAllNodesWithText("Skip setup").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Skip setup").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("TODAY").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("TODAY").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Add application").performClick()
        composeRule.onNodeWithText("Add an opportunity").assertIsDisplayed()
        composeRule.onNodeWithText("Company*").assertIsDisplayed()
        composeRule.onNodeWithText("Role*").assertIsDisplayed()
    }
}
