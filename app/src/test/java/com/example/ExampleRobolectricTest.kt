package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.ui.SocialMediaViewModel
import com.example.ui.screens.YarkhwoonApp
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun app_init_and_renders_successfully() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SocialMediaViewModel(application)

    composeTestRule.setContent {
      MyApplicationTheme {
        YarkhwoonApp(viewModel = viewModel)
      }
    }

    // Wait for compose to be idle to trigger any initial recompositions or animations
    composeTestRule.waitForIdle()
  }

  @Test
  fun signup_flow_wizard_transitions_successfully() {
    val application = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = SocialMediaViewModel(application)

    composeTestRule.setContent {
      MyApplicationTheme {
        YarkhwoonApp(viewModel = viewModel)
      }
    }

    // Wait until database prepopulation is 100% complete and currentUser is loaded
    composeTestRule.waitUntil(10000) {
      viewModel.allUsers.value.size >= 5 && viewModel.currentUser.value != null
    }
    composeTestRule.waitForIdle()

    // Since a new install prepopulates defaultCurrentUser (isProfileCompleted = false),
    // we are already on step 1 of SignUpAndProfileSetupScreen directly.

    // Fill step 1 details.
    composeTestRule.onNodeWithTag("signup_first_name").performTextInput("Rehan")
    composeTestRule.onNodeWithTag("signup_last_name").performTextInput("Khan")
    composeTestRule.onNodeWithTag("signup_username").performTextInput("rehan_khan")
    composeTestRule.waitForIdle()

    // Assert that the fields indeed contain the text to verify input was registered
    composeTestRule.onNodeWithTag("signup_first_name").assert(hasText("Rehan"))
    composeTestRule.onNodeWithTag("signup_last_name").assert(hasText("Khan"))
    composeTestRule.onNodeWithTag("signup_username").assert(hasText("rehan_khan"))

    composeTestRule.onNodeWithText("Step 1 of 3").assertExists()

    composeTestRule.onNodeWithTag("signup_step1_next").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Step 2 of 3").assertExists()

    // 3. We should be on step 2 now. Fill bio.
    composeTestRule.onNodeWithTag("signup_bio").performScrollTo().performTextInput("Love Chitrali culture!")
    composeTestRule.waitForIdle()

    // Click next step to step 3.
    composeTestRule.onNodeWithTag("signup_step2_next").performScrollTo().performClick()
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // 4. We should be on step 3. Check submit button exists.
    composeTestRule.onNodeWithTag("signup_submit").assertExists()
  }
}
