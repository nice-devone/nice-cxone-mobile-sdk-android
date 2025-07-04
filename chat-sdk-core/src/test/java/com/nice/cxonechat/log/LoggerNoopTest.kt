/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.log

import io.mockk.confirmVerified
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.PrintStream

internal class LoggerNoopTest {

    private lateinit var out: PrintStream

    @Before
    fun prepare() {
        out = mockk()
        System.setOut(out)
    }

    @After
    fun tearDown() {
        System.setOut(null)
    }

    @Test
    fun log_hasNoInteractions() {
        LoggerNoop.log(Level.Info, "")
        confirmVerified(out)
    }
}
