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

import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

internal class ChatLoggingMemoizeTest {

    private val origin = mockk<ChatWithParameters>(relaxed = true)
    private lateinit var chatLogging: ChatLogging

    @Before
    fun setUp() {
        chatLogging = ChatLogging(origin)
    }

    @Test
    fun `threads returns same instance on repeated calls`() {
        assertSame(chatLogging.threads(), chatLogging.threads())
    }

    @Test
    fun `events returns same instance on repeated calls`() {
        assertSame(chatLogging.events(), chatLogging.events())
    }

    @Test
    fun `customFields returns same instance on repeated calls`() {
        assertSame(chatLogging.customFields(), chatLogging.customFields())
    }

    @Test
    fun `actions returns same instance on repeated calls`() {
        assertSame(chatLogging.actions(), chatLogging.actions())
    }
}
