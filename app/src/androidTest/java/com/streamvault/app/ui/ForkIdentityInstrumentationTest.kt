package com.streamvault.app.ui

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.streamvault.app.BuildConfig
import com.streamvault.app.R
import com.streamvault.app.ui.screens.settings.SettingsUiState
import com.streamvault.app.ui.screens.settings.settingsAboutSection
import com.streamvault.app.ui.theme.Background
import com.streamvault.app.ui.theme.StreamVaultTheme
import java.io.File
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ForkIdentityInstrumentationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun installedIdentityAndLocalizedLabelsBelongToTheFork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertThat(context.packageName).isEqualTo("com.onsenturk.streamvault.debug")
        assertThat(context.packageName).isEqualTo(BuildConfig.APPLICATION_ID)
        assertThat(context.packageManager.getApplicationLabel(context.applicationInfo).toString())
            .isEqualTo("Clicker Debug")
        val provider = requireNotNull(
            context.packageManager.resolveContentProvider("${context.packageName}.fileprovider", 0)
        )
        assertThat(provider.packageName).isEqualTo(context.packageName)
        assertThat(provider.exported).isFalse()
        assertThat(provider.grantUriPermissions).isTrue()

        val languages = listOf(
            "en", "ar", "cs", "da", "de", "el", "es", "fi", "fr", "hu", "id", "it", "he",
            "ja", "ko", "nb", "nl", "pl", "pt", "ro", "ru", "sv", "tr", "uk", "vi", "zh"
        )
        languages.forEach { language ->
            val configuration = Configuration(context.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            }
            val localized = context.createConfigurationContext(configuration)
            assertThat(localized.getString(R.string.app_name)).isEqualTo("Clicker Debug")
            assertThat(localized.getString(R.string.fork_original_repository_url))
                .isEqualTo("https://github.com/Davidona/StreamVault-IPTV")
            assertThat(localized.getString(R.string.settings_donate_url)).isEqualTo("https://ko-fi.com/davidona")
        }
    }

    @Test
    fun aboutShowsIndependentMaintainerAndPreservesAccessibleOriginalCredits() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val openedUrls = mutableListOf<String>()
        composeRule.setContent {
            StreamVaultTheme {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(Background).safeDrawingPadding().testTag("fork-about"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    settingsAboutSection(
                        uiState = SettingsUiState(),
                        context = context,
                        buildVerificationLabel = context.getString(R.string.settings_build_verification_unavailable),
                        onOpenUri = { openedUrls += it },
                        onCheckForUpdates = {},
                        onInstallDownloadedUpdate = {},
                        onDownloadLatestUpdate = {},
                        onSetAutoCheckAppUpdates = {},
                        onSetAutoDownloadAppUpdates = {},
                        onRefreshDownloadState = {},
                        onViewCrashReport = {},
                        onShareCrashReport = {},
                        onDeleteCrashReport = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText(context.getString(R.string.app_name)).assertIsDisplayed()
        val lineage = context.getString(R.string.fork_identity_notice)
        assertThat(lineage).contains("Based on StreamVault")
        assertThat(lineage).contains("Onur (onsenturk)")
        composeRule.onNodeWithText(lineage).assertIsDisplayed()
        captureScreenshot("fork-maintainer")

        listOf(
            "https://github.com/onsenturk/Clicker",
            "https://github.com/Davidona/StreamVault-IPTV",
            "https://ko-fi.com/davidona"
        ).forEach { url ->
            composeRule.onNodeWithTag("fork-about").performScrollToNode(hasText(url))
            composeRule.onNodeWithText(url).performScrollTo().assertIsDisplayed().activateForDevice(context)
            composeRule.runOnIdle { assertThat(openedUrls.last()).isEqualTo(url) }
        }

        val credit = context.getString(R.string.fork_original_credit)
        assertThat(credit).contains("David Nashash (Davidona)")
        composeRule.onNodeWithTag("fork-about").performScrollToNode(hasText(credit))
        composeRule.onNodeWithText(credit).performScrollTo().assertIsDisplayed()
        captureScreenshot("original-credits")
    }

    private fun SemanticsNodeInteraction.activateForDevice(context: Context) {
        val isTelevision = context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK ==
            Configuration.UI_MODE_TYPE_TELEVISION
        if (isTelevision) {
            performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            performKeyInput { pressKey(Key.DirectionCenter) }
        } else {
            performTouchInput { click() }
        }
    }

    private fun captureScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val outputRoot = requireNotNull(InstrumentationRegistry.getArguments().getString("additionalTestOutputDir"))
        val output = File(outputRoot, "fork-identity").apply {
            check(isDirectory || mkdirs())
        }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(output, "$name.png").outputStream().use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            }
        } finally {
            bitmap.recycle()
        }
    }
}