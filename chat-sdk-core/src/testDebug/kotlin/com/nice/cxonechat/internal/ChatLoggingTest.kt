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

package com.nice.cxonechat.internal

import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * Regression test for the close()/closeSuspending() decorator pattern in [ChatLogging].
 *
 * [ChatLogging] is the outermost decorator in the production stack; if its [ChatLogging.close]
 * short-circuited by calling `origin.close()` directly instead of `origin.closeSuspending()`,
 * the whole close/drain chain would be bypassed — every intermediate decorator's cleanup would
 * be skipped, and storage/cookie writes would not be awaited before the method returned.
 */
internal class ChatLoggingTest {

    /**
     * Plain (non-runTest) test — close() bridges via runBlocking, which must never be called
     * from within a runTest coroutine. Using a relaxed mock for origin satisfies the
     * LoggerScope construction (origin.entrails.logger) and the socketListener.addListener()
     * call in ChatLogging's init block without requiring a real implementation.
     */
    @Test
    fun `close cascades to origin closeSuspending, never origin close`() {
        val origin = mockk<ChatWithParameters>(relaxed = true)
        val decorator = ChatLogging(origin)

        decorator.close()

        coVerify(exactly = 1) { origin.closeSuspending() }
        verify(exactly = 0) { origin.close() }
    }
}
