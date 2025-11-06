/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.nice.cxonechat.Popup
import com.nice.cxonechat.message.Action
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.preview.message.UiSdkReplyButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
@LargeTest
class InactivityPopupTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createPopup(startedAt: Date, timeoutSeconds: Long): Popup.InactivityPopup {
        return object : Popup.InactivityPopup {
            override val title: String = "Your chat will expire in"
            override val body: String = "When the time expires, the conversation is terminated. Would you like to continue?"
            override val countdown: Popup.InactivityPopup.Countdown = object : Popup.InactivityPopup.Countdown {
                override val timeoutSeconds: Long = timeoutSeconds
                override val startedAt: Date = startedAt
            }
            override val callToAction: String = "Please respond to continue."
            override val sessionExpire: Action = UiSdkReplyButton("Close Chat")
            override val sessionRefresh: Action = UiSdkReplyButton("Continue")
        }
    }

    @Test
    fun showsActiveTitleAndActions_whenCountdownActive() {
        val popup = createPopup(Date(System.currentTimeMillis()), 10)
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
        val popup = createPopup(Date(System.currentTimeMillis() - 20_000), 1)
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
        val start = System.currentTimeMillis()
        val timeoutSeconds = 2L
        val popup = createPopup(Date(start), timeoutSeconds)
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
        val popup = createPopup(Date(System.currentTimeMillis()), 10)
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
        val popup = createPopup(Date(System.currentTimeMillis()), 10)
        composeTestRule.setContent {
            ChatTheme {
                InactivityPopup(
                    popup,
                    onClickAction = {},
                    closeChat = { closeCalled.set(true) }
                )
            }
        }
        pressBack() // Use Espresso to simulate back press
        composeTestRule.waitForIdle()
        assert(closeCalled.get())
    }
}
