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

import com.nice.cxonechat.ChatState
import org.junit.Test
import kotlin.test.assertEquals

internal class ChatStateExtTest {

    @Test
    fun isAtLeastPrepared_pinsDocumentedContract() {
        val expected = mapOf(
            ChatState.Initial to false,
            ChatState.Preparing to false,
            ChatState.Prepared to true,
            ChatState.Connecting to true,
            ChatState.Connected to true,
            ChatState.Ready to true,
            ChatState.Offline to false,
            ChatState.ConnectionLost to false,
            ChatState.SdkNotSupported to false,
        )

        expected.forEach { (state, expectedResult) ->
            assertEquals(expectedResult, state.isAtLeastPrepared(), "isAtLeastPrepared() for $state")
        }
    }
}
