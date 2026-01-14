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

package com.nice.cxonechat.ui.composable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.nice.cxonechat.ui.composable.generic.MessageAvatar
import com.nice.cxonechat.ui.domain.model.Person
import org.junit.Rule
import org.junit.Test

class MessageAvatarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun messageAvatar_displaysImage_whenImageUrlProvided() {
        val person = Person(imageUrl = "https://brand-embassy-avatars-qa.s3.eu-west-1.amazonaws.com/324bcba7-f317-4a99-b833-06a1952c41c6.jpg")
        composeTestRule.setContent {
            MessageAvatar(agent = person)
        }
        composeTestRule.onNodeWithTag("avatarImage").assertIsDisplayed()
        composeTestRule.onNodeWithText("SJ").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("avatarPlaceholder").assertDoesNotExist()
    }

    @Test
    fun messageAvatar_displaysMonogram_whenMonogramProvided() {
        val person = Person(firstName = "Steve", lastName = "Jobs")
        composeTestRule.setContent {
            MessageAvatar(agent = person)
        }
        composeTestRule.onNodeWithText("SJ").assertIsDisplayed()
        composeTestRule.onNodeWithTag("avatarImage").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("avatarPlaceholder").assertDoesNotExist()
    }

    @Test
    fun messageAvatar_displaysPlaceholder_whenNoImageOrMonogram() {
        val person = Person()
        composeTestRule.setContent {
            MessageAvatar(agent = person)
        }
        composeTestRule.onNodeWithContentDescription("avatarPlaceholder").assertIsDisplayed()
        composeTestRule.onNodeWithTag("avatarImage").assertDoesNotExist()
        composeTestRule.onNodeWithText("SJ").assertDoesNotExist()
    }
}
