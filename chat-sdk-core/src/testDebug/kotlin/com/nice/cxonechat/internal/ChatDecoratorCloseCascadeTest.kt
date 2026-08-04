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
 * Regression test for the close()/closeSuspending() dual-method decorator pattern: calling the
 * non-suspend close() on an outer decorator must cascade through every intermediate decorator's
 * closeSuspending() down to the innermost origin -- never skip a layer or call origin.close()
 * directly (which would bypass whatever cleanup a middle layer's closeSuspending() performs).
 */
internal class ChatDecoratorCloseCascadeTest {
    @Test
    fun `close() on the outermost decorator cascades via closeSuspending() through every layer`() {
        val innermost = mockk<ChatWithParameters>(relaxed = true)
        val middle = ChatWelcomeMessageUpdate(innermost)
        val outer = ChatServerErrorReporting(middle)

        outer.close()

        // The innermost origin must receive closeSuspending() -- never the blocking close() --
        // proving the chain wasn't short-circuited past the middle decorator.
        coVerify(exactly = 1) { innermost.closeSuspending() }
        verify(exactly = 0) { innermost.close() }
    }
}
