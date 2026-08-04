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

import android.os.SystemClock
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.text.intl.Locale.Companion.current
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.Popup.InactivityPopup
import com.nice.cxonechat.Popup.InactivityPopup.Countdown
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkReplyButton
import com.nice.cxonechat.ui.util.toTimeStamp
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock.System
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

@RunWith(AndroidJUnit4::class)
@LargeTest
class InactivityPopupTest : AbstractComponentActivityUiTest() {

    private fun createPopup(startedAt: Instant, timeoutSeconds: Long): InactivityPopup {
        return object : InactivityPopup {
            override val title: String = "Your chat will expire in"
            override val body: String = "When the time expires, the conversation is terminated. Would you like to continue?"
            override val countdown: Countdown = object : Countdown {
                override val timeoutSeconds: Long = timeoutSeconds
                override val startedAt: Instant = startedAt
            }
            override val callToAction: String = "Please respond to continue."
            override val sessionExpire: Action = UiSdkReplyButton("Close Chat")
            override val sessionRefresh: Action = UiSdkReplyButton("Continue")
        }
    }

    @Test
    fun showsActiveTitleAndActions_whenCountdownActive() {
        val popup = createPopup(System.now(), 10)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(popup, {}, {})
            }
        }
        composeTestRule.onNodeWithTag("inactivity_popup").assertIsDisplayed()
        composeTestRule.onNodeWithTag("inactivity_popup_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("inactivity_popup_refresh_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("inactivity_popup_expire_button").assertIsDisplayed()
    }

    @Test
    fun showsExpiredTitle_whenCountdownExpired() {
        val popup = createPopup(System.now() - 20_000.toDuration(DurationUnit.MILLISECONDS), 1)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(popup, {}, {})
            }
        }
        composeTestRule.onNodeWithTag("inactivity_popup").assertIsDisplayed()
        composeTestRule.onNodeWithTag("inactivity_popup_title").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("inactivity_expired_title").assertIsDisplayed()
    }

    @Test
    fun switchesState_whenCounterReachesZero() {
        val start = System.now()
        val timeoutSeconds = 2L
        val popup = createPopup(start, timeoutSeconds)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(popup, {}, {})
            }
        }
        composeTestRule.onNodeWithTag("inactivity_popup_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("inactivity_expired_title").assertIsNotDisplayed()
        val ms = timeoutSeconds * 1_000
        composeTestRule.waitUntil(ms + 1) {
            SystemClock.sleep(ms) // The calculation is using real clock - we need to wait
            true
        }
        composeTestRule.mainClock.advanceTimeBy(1_000) // Advance time for LaunchedEffect to recalculate
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("inactivity_popup_title").assertIsNotDisplayed()
        composeTestRule.onNodeWithTag("inactivity_expired_title").assertIsDisplayed()
    }

    @Test
    fun actionButtonsCallCallbacks() {
        val refreshCalled = AtomicBoolean(false)
        val expireCalled = AtomicBoolean(false)
        val popup = createPopup(System.now(), 10)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(
                    popup,
                    onClickAction = {
                        if (it == popup.sessionRefresh) refreshCalled.set(true)
                        if (it == popup.sessionExpire) expireCalled.set(true)
                    },
                    closeChat = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("inactivity_popup_refresh_button").performClick()
        composeTestRule.onNodeWithTag("inactivity_popup_expire_button").performClick()
        assert(refreshCalled.get())
        assert(expireCalled.get())
    }

    @Test
    fun dismissCallsCloseChatCallback() {
        val closeCalled = AtomicBoolean(false)
        val popup = createPopup(System.now(), 10)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(
                    popup,
                    onClickAction = {},
                    closeChat = { closeCalled.set(true) }
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("inactivity_popup_title").assertIsDisplayed()
        pressBack() // Use Espresso to simulate back press
        composeTestRule.waitForIdle()
        assert(closeCalled.get())
    }

    /**
     * Test that the inactivity popup has proper accessibility support when active.
     */
    @Test
    fun inactivityPopup_activeState_hasAccessibility() {
        val popup = createPopup(System.now(), 10)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(popup, {}, {})
            }
        }

        composeTestRule.onNodeWithTag("inactivity_popup")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("inactivity_popup_title")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that the inactivity popup has proper accessibility support when expired.
     */
    @Test
    fun inactivityPopup_expiredState_hasAccessibility() {
        val popup = createPopup(System.now() - 20_000.toDuration(DurationUnit.MILLISECONDS), 1)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(popup, {}, {})
            }
        }
        val titleText = getString(string.inactivity_time_up_title)
        val subtitleText = getString(string.inactivity_time_up_subtitle)
        val accessibilityText = "$titleText $subtitleText"
        composeTestRule.onNodeWithTag("inactivity_expired_title")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
            .assertContentDescriptionEquals(accessibilityText)
    }

    /**
     * Test that action buttons have proper accessibility.
     */
    @Test
    fun inactivityPopup_actionButtons_haveAccessibility() {
        val popup = createPopup(System.now(), 10)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(popup, {}, {})
            }
        }

        composeTestRule.onNodeWithTag("inactivity_popup_refresh_button")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag("inactivity_popup_expire_button")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    /**
     * Test that all interactive elements in the popup are accessible.
     */
    @Test
    fun inactivityPopup_allInteractiveElements_areAccessible() {
        val popup = createPopup(System.now(), 30)
        // Capture the offset between wall-clock epoch ms and the Compose test clock before
        // setContent. After composition + autoAdvance=false, mainClock.currentTime is frozen at
        // the last composition frame, so (clockDiff + mainClock.currentTime) reconstructs the
        // wall-clock instant when Clock.System.now() ran inside the composable — robust to any
        // amount of delay before or after setContent.
        val clockDiff = popup.countdown.startedAt.toEpochMilliseconds() - composeTestRule.mainClock.currentTime
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(popup, {}, {})
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false
        val now = Instant.fromEpochMilliseconds(clockDiff + composeTestRule.mainClock.currentTime)
        val end = popup.countdown.startedAt +
                popup.countdown.timeoutSeconds.toDuration(DurationUnit.SECONDS)

        // Test the main container
        composeTestRule.onNodeWithTag("inactivity_popup")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Test the title
        val duration = (end - now).setMinDuration()
        val locale = current
        val title = popup.title + " " + duration.toTimeStamp(locale)
        val subtitle = popup.body + " " + popup.callToAction
        val titleDescription = "$title, $subtitle"
        composeTestRule.onNodeWithTag("inactivity_popup_title")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
            .assertContentDescriptionEquals(titleDescription)

        // Test refresh button
        composeTestRule.onNodeWithTag("inactivity_popup_refresh_button")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Test expire button
        composeTestRule.onNodeWithTag("inactivity_popup_expire_button")
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }
}
