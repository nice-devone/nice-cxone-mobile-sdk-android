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

internal class ChatWelcomeMessageUpdateTest {

    /**
     * Plain (non-runTest) test, matching ChatDecoratorCloseCascadeTest -- close() bridges via
     * runBlocking, which must never be called from within a runTest coroutine.
     */
    @Test
    fun `close cascades to origin closeSuspending, never origin close`() {
        val origin = mockk<ChatWithParameters>(relaxed = true)
        val decorator = ChatWelcomeMessageUpdate(origin)

        decorator.close()

        coVerify(exactly = 1) { origin.closeSuspending() }
        verify(exactly = 0) { origin.close() }
    }
}
