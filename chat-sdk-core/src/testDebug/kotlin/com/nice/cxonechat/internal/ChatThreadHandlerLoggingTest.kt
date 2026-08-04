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

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.log.Logger
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Test

internal class ChatThreadHandlerLoggingTest {

    private val logger = mockk<Logger>(relaxed = true)
    private val origin = mockk<ChatThreadHandler>(relaxed = true)
    private val handler = ChatThreadHandlerLogging(origin, logger)

    @Test
    fun `messages returns same instance on repeated calls`() {
        assertSame(handler.messages(), handler.messages())
    }

    @Test
    fun `events returns same instance on repeated calls`() {
        assertSame(handler.events(), handler.events())
    }

    @Test
    fun `actions returns same instance on repeated calls`() {
        assertSame(handler.actions(), handler.actions())
    }

    @Test
    fun `customFields returns same instance on repeated calls`() {
        assertSame(handler.customFields(), handler.customFields())
    }
}
