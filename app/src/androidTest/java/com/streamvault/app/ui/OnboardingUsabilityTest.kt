package com.streamvault.app.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelStore
import com.streamvault.app.R
import com.google.common.truth.Truth.assertThat
import com.streamvault.app.ui.screens.provider.ProviderSetupScreen
import com.streamvault.app.ui.screens.provider.ProviderSetupViewModel
import com.streamvault.app.ui.theme.StreamVaultTheme
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class OnboardingUsabilityTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val viewModels = ViewModelStore()

    @After
    fun cleanUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModels.clear() }
    }

    @Test
    fun providerSetup_allSourcesAndRestoreAreReachable() {
        val activity = showSetup()

        composeRule.onNodeWithText("Add from phone").assertDoesNotExist()
        listOf(
            R.string.setup_xtream,
            R.string.setup_stalker,
            R.string.setup_tab_url,
            R.string.setup_tab_file,
            R.string.setup_tab_jellyfin
        ).forEach { source ->
            composeRule.onNodeWithText(activity.getString(source))
                .performScrollTo()
                .assertIsDisplayed()
                .activateForDevice(activity)
            composeRule.waitForIdle()
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = File(requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")), "usability")
        output.mkdirs()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(output, "provider-sources.png").outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        } finally {
            bitmap.recycle()
        }

        composeRule.onNodeWithContentDescription(activity.getString(R.string.settings_restore_data))
            .apply { if (!isDisplayed()) performScrollTo() }
            .assertIsDisplayed()
            .activateForDevice(activity)
        composeRule.waitForIdle()
    }

    @Test
    fun providerUrl_preservesRapidKeyboardInput() {
        val context = showSetup()
        composeRule.onNodeWithText(context.getString(R.string.setup_tab_url))
            .performScrollTo().activateForDevice(context)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(context.getString(R.string.setup_m3u_hint))
            .performScrollTo().activateForDevice(context)
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction() and isFocused()).assertIsFocused()
        val url = "https://example.test/playlist.m3u?username=review&password=fixture"

        InstrumentationRegistry.getInstrumentation().sendStringSync(url)

        composeRule.waitForIdle()
        val editorValues = composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)
            .fetchSemanticsNodes().map { node -> node.config[SemanticsProperties.EditableText].text }
        assertThat(editorValues).contains(url)
    }

    private fun SemanticsNodeInteraction.activateForDevice(context: Context) {
        val isTelevision = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_TELEVISION
        if (isTelevision) {
            performSemanticsAction(SemanticsActions.RequestFocus) { requestFocus -> requestFocus() }
            performKeyInput { pressKey(Key.DirectionCenter) }
        } else {
            performTouchInput { click() }
        }
    }

    private fun showSetup(): Context {
        val activity = ApplicationProvider.getApplicationContext<Context>()
        val dependencies = EntryPointAccessors.fromApplication(activity, ProviderSetupTestEntryPoint::class.java)
        lateinit var viewModel: ProviderSetupViewModel
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = ProviderSetupViewModel(
                dependencies.providerRepository(),
                dependencies.combinedM3uRepository(),
                dependencies.validateAndAddProvider(),
                dependencies.importBackup(),
                dependencies.driveBackupSyncManager()
            )
            viewModels.put("setup", viewModel)
        }
        composeRule.setContent {
            StreamVaultTheme {
                ProviderSetupScreen(onProviderAdded = {}, onBack = {}, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        return activity
    }
}