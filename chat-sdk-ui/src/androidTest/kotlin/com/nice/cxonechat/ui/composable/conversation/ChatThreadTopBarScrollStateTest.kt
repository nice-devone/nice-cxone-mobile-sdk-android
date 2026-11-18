/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.composable.conversation.model.ConversationTopBarState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers which bar [ChatThreadTopBar] draws for a window, and what it does with the scroll behavior it is
 * handed.
 *
 * The wide window's bar is pinned and takes no scroll behavior at all, so the caller has to supply one that
 * is already pinned — `rememberTopBarScrollBehavior` does that. This pins the half of the arrangement that
 * only shows on a device: which of the two bars was drawn. `TopBarScrollBehaviorTest` covers the other half,
 * that the behavior chosen for a wide window consumes no scroll delta.
 *
 * Every other test for this bar passes a null scroll behavior, which never reaches the branch — and three
 * attempts at it were plausible enough to ship while being wrong, each caught by reading the Material 3
 * source rather than by a failing test.
 *
 * A real rotation is not needed: the branch is chosen by window size, so faking a wide window is enough.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class ChatThreadTopBarScrollStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Both bars carry the same tag, so their height is what says which one was drawn: the wide window gets a
     * single row and the compact one the taller two-row bar. Asserted as a comparison rather than against the
     * Material 3 container heights, which are the library's to change.
     */
    @Test
    fun wideWindowDrawsAShorterBarThanACompactWindow() {
        val windowWidth = mutableStateOf(EXPANDED_WIDTH)
        showTopBar(expandedState(), windowWidth)

        val wideHeight = barHeight()

        composeTestRule.runOnIdle { windowWidth.value = COMPACT_WIDTH }
        composeTestRule.waitForIdle()

        val compactHeight = barHeight()

        assertTrue(
            "The compact window should draw the two-row bar, taller than the wide window's single row, " +
                "but the wide one measured $wideHeight and the compact one $compactHeight",
            compactHeight > wideHeight,
        )
    }

    /**
     * The wide bar never receives the behavior, so a collapsed offset survives untouched. This is the trap
     * the parameter's documentation warns about: handing it an enter-always behavior compiles and does
     * nothing here, while still driving the scaffold's nested scroll.
     */
    @Test
    fun wideBarLeavesTheScrollStateItIsHandedAlone() {
        val state = collapsedState()

        showTopBar(state, EXPANDED_WIDTH)

        composeTestRule.runOnIdle {
            assertEquals("The pinned wide bar should not write the caller's offset", COLLAPSED_OFFSET, state.heightOffset)
            assertEquals("The pinned wide bar should not write the caller's limit", COLLAPSED_OFFSET, state.heightOffsetLimit)
        }
    }

    @Test
    fun compactBarKeepsTheOffsetSoItCanCollapse() {
        val state = collapsedState()

        showTopBar(state, COMPACT_WIDTH)

        composeTestRule.runOnIdle {
            assertEquals("The two-row bar owns its offset and must keep it", COLLAPSED_OFFSET, state.heightOffset)
        }
    }

    private fun barHeight(): Dp =
        composeTestRule.onNodeWithTag("chat_thread_top_bar").getUnclippedBoundsInRoot().height

    /** Composes the bar in a window of [windowWidth], which stays that size for the rest of the test. */
    private fun showTopBar(state: TopAppBarState, windowWidth: Dp) = showTopBar(state, mutableStateOf(windowWidth))

    /**
     * Composes the bar in a window of [windowWidth], which a test may change to re-measure in another.
     *
     * A fixed window per test would be simpler, and `ChatThemeTopBarTest` covers this breakpoint that way.
     * It cannot work here: `setContent` may only be called once per rule, so measuring both bars in one
     * assertion needs the window to move underneath a live composition. That leans on
     * `DeviceConfigurationOverride.WindowSize` re-applying when its size changes — it does, because it
     * reports the size through `rememberUpdatedState`, which a composable reading `containerDpSize`
     * subscribes to.
     */
    private fun showTopBar(state: TopAppBarState, windowWidth: MutableState<Dp>) {
        composeTestRule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(windowWidth.value, WINDOW_HEIGHT)),
            ) {
                ChatTheme {
                    // The bar has to sit in a container that measures it with a loose height, as the
                    // scaffold's top bar slot does. Composed directly under the override it is handed a
                    // fixed height instead — the override forces its content to the window size — and then
                    // reports the whole window height whichever bar was drawn.
                    Column {
                        ChatThreadTopBar(
                            scrollBehavior = enterAlwaysScrollBehavior(state),
                            conversationState = ConversationTopBarState(
                                threadName = flowOf("Test"),
                                isMultiThreaded = false,
                                hasQuestions = false,
                                isLiveChat = false,
                                liveChatAllowTranscript = false,
                                isArchived = MutableStateFlow(false),
                                threadState = MutableStateFlow(ChatThreadState.Ready),
                            ),
                            onEditThreadName = {},
                            onEditThreadValues = {},
                            onEndContact = {},
                            displayEndConversation = {},
                            onSendTranscript = {},
                        )
                    }
                }
            }
        }
    }

    private companion object {
        /** Wide enough for the single row, and portrait so the breakpoint decides rather than orientation. */
        val EXPANDED_WIDTH = 1280.dp

        val COMPACT_WIDTH = 411.dp

        val WINDOW_HEIGHT = 1600.dp

        /** A plausible collapsed offset; the exact value only has to be non-zero. */
        const val COLLAPSED_OFFSET = -96f

        /**
         * Already collapsed, as the two-row bar leaves it after the conversation is scrolled. Built rather
         * than seeded during composition, so the offset is a precondition the bar is handed; writing it from
         * the composable body would repeat on every recomposition and the assertions would then be about
         * that write.
         */
        fun collapsedState() = TopAppBarState(
            initialHeightOffsetLimit = COLLAPSED_OFFSET,
            initialHeightOffset = COLLAPSED_OFFSET,
            initialContentOffset = 0f,
        )

        /** Nothing scrolled yet, so the two-row bar draws at its full height. */
        fun expandedState() = TopAppBarState(
            initialHeightOffsetLimit = 0f,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
    }
}
