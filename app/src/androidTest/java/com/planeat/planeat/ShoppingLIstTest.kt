package com.planeat.planeat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.compose.AppTheme
import com.planeat.planeat.ui.ShoppingScreen
import org.junit.Rule
import org.junit.Test

class ShoppingLIstTest {

    @get:Rule val composeTestRule = createComposeRule()


    @Test
    fun addIngredientToShoppingList() {
        composeTestRule.setContent {
            AppTheme {
                ShoppingScreen(onRecipeSelected = {}, goToAgenda = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Add ingredient").performClick()

        composeTestRule.onNodeWithText("Carrots, Eggs, Chocolate…")
                       .assertIsDisplayed().assertIsFocused()
        composeTestRule.onNodeWithTag("search_input").performTextInput("banana")

        composeTestRule.onAllNodesWithTag("add_ingredient").onLast().assertIsDisplayed().performClick()

        composeTestRule.waitUntil(3000) {
            composeTestRule.onNodeWithText("Aisle").isDisplayed()
        }

        composeTestRule.onAllNodesWithText("Banana").onLast().assertIsDisplayed()
        composeTestRule.onNodeWithText("Aisle").assertIsDisplayed()
    }


}