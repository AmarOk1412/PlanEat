
import android.os.Build
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import com.planeat.planeat.ui.MainActivity
import org.junit.Rule
import org.junit.Test

// file: app/src/androidTest/java/com/package/MyComposeTest.kt

class PlanEatTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    // use createAndroidComposeRule<YourActivity>() if you need access to
    // an activity

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    fun currentWindowAdaptiveInfo(): WindowSizeClass {
        val configuration = LocalConfiguration.current
        val size = DpSize(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
        return WindowSizeClass.calculateFromSize(size)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun testSaveRecipe() {
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

}

