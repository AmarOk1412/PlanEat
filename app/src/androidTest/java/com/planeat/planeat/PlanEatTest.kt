
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.planeat.planeat.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

// file: app/src/androidTest/java/com/package/MyComposeTest.kt

class PlanEatTest {

    class CopyFileBeforeLaunchRule : TestRule {

        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {

                fun copyTestAssetToAppStorage(context: Context, fileName: String, targetPath: String) {
                    val assetManager = context.assets
                    val inputStream: InputStream = assetManager.open(fileName)
                    val outputFile = File(targetPath)

                    // Make sure the directories exist
                    outputFile.parentFile?.mkdirs()

                    val outputStream = FileOutputStream(outputFile)
                    inputStream.copyTo(outputStream)

                    inputStream.close()
                    outputStream.close()
                }

                @Throws(Throwable::class)
                override fun evaluate() {
                    // Get the context for the instrumentation test (access to test assets)
                    val testContext = InstrumentationRegistry.getInstrumentation().context
                    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

                    // Path where the application loads the suggestions.json from (e.g., internal storage)
                    val suggestionsFilePath = File(appContext.filesDir, "suggestions.json").path

                    // Copy the suggestions.json from androidTest/assets to internal storage
                    copyTestAssetToAppStorage(testContext, "suggestions.json", suggestionsFilePath)

                    // Proceed with the test
                    base.evaluate()
                }
            }
        }
    }

    @get:Rule
    val copyFileBeforeLaunchRule = CopyFileBeforeLaunchRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testFilterSuggestion() {
        composeTestRule.onNodeWithText("Discover",).performClick()
        composeTestRule.onNodeWithText("You may like").assertIsDisplayed()

        // Click on Drinks
        composeTestRule.onNodeWithText("Drinks").performScrollTo().performClick()
        composeTestRule.onNodeWithText("You may like").assertIsNotDisplayed()

        composeTestRule.onNodeWithText("European").performScrollTo().performClick()
        composeTestRule.onNodeWithText("You may like").assertIsDisplayed()

        composeTestRule.onAllNodesWithTag("favorite_button").onFirst().assertExists()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun addNewRecipe() {
        composeTestRule.onNodeWithText("Favorites",).performClick()
        composeTestRule.onNodeWithContentDescription("New Recipe").assertIsDisplayed()
        composeTestRule.onNodeWithText("My recipes").assertIsNotDisplayed()
        composeTestRule.onNodeWithContentDescription("New Recipe").performClick()

        // Save recipe
        // Add "Foo" as the title
        composeTestRule.onNodeWithTag("title_input") // Add a test tag to the title TextField
            .performTextInput("Foo")

        // Set "60" as cooking time
        composeTestRule.onNodeWithTag("cooking_time_input") // Add a test tag to the cooking time TextField
            .performTextInput("60")

        // Click on Save button
        composeTestRule.onNodeWithText("Save").performScrollTo()
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save") // If button text is Save
            .performClick()

        // Optionally, verify that the recipe is saved and updated correctly by checking if "Foo" is displayed
        composeTestRule.onNodeWithText("My recipes").assertIsDisplayed()
        composeTestRule.onNodeWithText("Foo").assertIsDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun saveSuggestion() {
        // Recipes should be empty
        composeTestRule.onAllNodesWithText("Favorites",).onLast().performClick()
        composeTestRule.onNodeWithText("Favorite").assertIsNotDisplayed()

        composeTestRule.onNodeWithText("Discover",).performClick()
        composeTestRule.onNodeWithText("You may like").assertIsDisplayed()

        // Save one recipe
        composeTestRule.onAllNodesWithTag("favorite_button").onFirst().performClick()

        // Should appear in favorites
        composeTestRule.onNodeWithText("Favorites",).performClick()
        composeTestRule.onNodeWithText("Favorite").assertIsDisplayed()
    }

    fun filterRecipes() {
        composeTestRule.onAllNodesWithText("Favorites",).onLast().performClick()
        composeTestRule.onNodeWithText("Favorite").assertIsDisplayed()

        // Click on European (one saved from previous test)
        composeTestRule.onNodeWithText("European").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithText("My recipes").assertIsNotDisplayed()

        // Click on Drinks (one saved from previous test)
        composeTestRule.onNodeWithText("Drinks").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Favorite").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("My recipes").assertIsNotDisplayed()

        // All, everything is shown
        composeTestRule.onNodeWithText("All").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Favorite").assertIsDisplayed()
        composeTestRule.onNodeWithText("My recipes").assertIsDisplayed()
    }

    fun searchAndAddEggs() {
        composeTestRule.onNodeWithText("Discover",).performClick()
        composeTestRule.onNodeWithText("You may like").assertIsDisplayed()

        // Search for "eggs"
        composeTestRule.onNodeWithTag("search_input") // Add a test tag to the search TextField
            .performTextInput("eggs")

        // Save one recipe
        composeTestRule.onNodeWithText("New recipes",).assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("favorite_button").onLast().performClick()

        // Should have 2 recipes in favorites + 1 in my recipes
        composeTestRule.onAllNodesWithText("Favorites",).onLast().performClick()
        composeTestRule.onNodeWithText("Favorite").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("favorite_button").assertCountEquals(3)

    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testScenarios() {
        addNewRecipe()
        saveSuggestion()
        filterRecipes()
        // TODO searchAndAddEggs()
    }

}

