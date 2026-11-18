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

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression coverage for DE-178452: [ErrorGroup.blocksAutoReconnect] is the single source of truth
 * [com.nice.cxonechat.ui.composable.ChatStateEffect] reads to suppress auto-reconnect, so it must stay
 * exactly in sync with the non-dismissible groups [com.nice.cxonechat.ui.screen.ChatErrorScreen] renders.
 * [ErrorGroup.SILENT_EXIT] also ends the chat session but renders no blocking dialog, so it must stay `false`.
 */
class ErrorGroupTest {

    @Test
    fun `blocksAutoReconnect is true only for HIGH and HIGH_SPECIFIC`() {
        val expected = mapOf(
            ErrorGroup.DO_NOTHING to false,
            ErrorGroup.LOW to false,
            ErrorGroup.LOW_SPECIFIC to false,
            ErrorGroup.HIGH to true,
            ErrorGroup.HIGH_SPECIFIC to true,
            ErrorGroup.SILENT_EXIT to false,
        )

        val actual = ErrorGroup.entries.associateWith { it.blocksAutoReconnect }

        assertEquals(expected, actual)
    }
}
