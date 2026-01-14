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

package com.nice.cxonechat.ui.util

import android.os.Build
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [Build.VERSION_CODES.N],
)
class EmojiCompatExtTest {

    lateinit var emojiCompat: EmojiCompat

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        emojiCompat = EmojiCompat.init(BundledEmojiCompatConfig(context, Executors.newSingleThreadExecutor()))
        requireNotNull(emojiCompat)
        check(emojiCompat.loadState == 0 || emojiCompat.loadState == 1) {
            "EmojiCompat is not loading"
        }
        var waiting = 10_000L
        while (emojiCompat.loadState == 0 && waiting > 0) {
            Thread.sleep(100)
            waiting -= 100
        }
        check(emojiCompat.loadState == 1) {
            "EmojiCompat failed to load"
        }
    }

    @Test
    fun emojiCountTest() {
        val message = "😊😊😊"
        val limit = 3
        val result = emojiCompat.emojiCount(message, limit)
        assertEquals(3, result)
    }

    @Test
    fun emojiCount_exceedLimit() {
        val message = "😊😊😊😊"
        val limit = 3
        val result = emojiCompat.emojiCount(message, limit)
        assertEquals(-1, result)
    }

    @Test
    fun emojiCount_nonEmoji() {
        val message = "Hello 👋, this is a test message with emojis 😊😊😊"
        val limit = 2
        val result = emojiCompat.emojiCount(message, limit)
        assertEquals(-1, result)
    }
}
