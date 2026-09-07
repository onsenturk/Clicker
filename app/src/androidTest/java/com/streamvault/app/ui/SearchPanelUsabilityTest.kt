package com.streamvault.app.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.streamvault.app.R
import com.streamvault.app.ui.screens.search.SearchHeroPanel
import com.streamvault.app.ui.screens.search.SearchTab
import com.streamvault.app.ui.theme.StreamVaultTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchPanelUsabilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactSearch_leavesSpaceForResultsAndKeepsInputAndFilters() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue(
            "The compact search layout is phone-only",
            context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK != Configuration.UI_MODE_TYPE_TELEVISION
        )
        var selectedTab = SearchTab.ALL
        composeRule.setContent {
            val query = remember { mutableStateOf("France") }
            StreamVaultTheme {
                SearchHeroPanel(
                    query = query.value,
                    selectedTab = selectedTab,
                    recentQueries = listOf("Earlier query"),
                    totalResults = 15,
                    onQueryChange = { query.value = it },
                    onSearch = {},
                    onTabSelected = { selectedTab = it },
                    onRecentQuerySelected = {},
                    onClearRecentQueries = {},
                    focusRequester = remember { FocusRequester() },
                    selectedStateLabel = "Selected",
                    compactLayout = true,
                    modifier = Modifier.testTag("compact-search")
                )
            }
        }

        val heightPixels = composeRule.onNodeWithTag("compact-search").fetchSemanticsNode().boundsInRoot.height
        assertThat(heightPixels).isAtMost(190f * context.resources.displayMetrics.density)
        composeRule.onNodeWithText(context.getString(R.string.search_command_subtitle)).assertDoesNotExist()
        composeRule.onNodeWithText("Earlier query").assertDoesNotExist()
        composeRule.onNodeWithContentDescription(context.getString(R.string.search_hint))
            .assertIsDisplayed().performTouchInput { click() }
        composeRule.onNode(hasSetTextAction()).performTextInput(" 24")
        composeRule.onNodeWithText("France 24").assertExists()
        composeRule.onNodeWithText(context.getString(R.string.search_movies))
            .assertIsDisplayed().performTouchInput { click() }
        composeRule.runOnIdle { assertThat(selectedTab).isEqualTo(SearchTab.MOVIES) }
    }
}