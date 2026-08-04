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

package com.nice.cxonechat.ui.composable.screenshot

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.ui.composable.ChatThreadView
import com.nice.cxonechat.ui.composable.PreviewThread
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.Thread
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.GraphicsMode.Mode

/**
 * Verifies that the ThreadList row's avatar follows the same image -> initials -> placeholder
 * fallback chain used for message avatars in the conversation ([com.nice.cxonechat.ui.composable.generic.MessageAvatar]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-normal-notlong-notround-notnight-mdpi-finger")
@GraphicsMode(Mode.NATIVE)
class ThreadListAvatarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `thread list row displays initials when image is hidden but name is visible`() {
        renderThread(agent("Alice", "Smith", imageUrl = null))
    }

    @Test
    fun `thread list row displays placeholder when both image and name are hidden`() {
        renderThread(agent(firstName = null, lastName = null, imageUrl = null))
    }

    private fun renderThread(agent: Agent) {
        val thread = PreviewThread(threadName = "", threadAgent = agent, messages = emptyList())
        composeTestRule.setContent {
            ChatTheme {
                Surface(Modifier.fillMaxWidth()) {
                    ChatThreadView(thread = Thread(thread, name = null), onThreadSelected = {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    private fun agent(firstName: String?, lastName: String?, imageUrl: String?): Agent = object : Agent() {
        override val id = 1
        override val firstName: String? = firstName
        override val lastName: String? = lastName
        override val nickname: String? = null
        override val isBotUser: Boolean = false
        override val isSurveyUser: Boolean = false
        override val imageUrl: String? = imageUrl
        override val isTyping = false
    }
}
