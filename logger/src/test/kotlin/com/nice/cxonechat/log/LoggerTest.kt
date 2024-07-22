/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.log.Level.Debug
import com.nice.cxonechat.log.Level.Error
import com.nice.cxonechat.log.Level.Info
import com.nice.cxonechat.log.Level.Verbose
import com.nice.cxonechat.log.Level.Warning
import com.nice.cxonechat.log.fake.CollectingLogger
import com.nice.cxonechat.log.fake.LoggedMessage
import com.nice.cxonechat.log.tool.nextString
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

internal class LoggerTest {

    private lateinit var logger: CollectingLogger

    @Before
    fun prepare() {
        logger = CollectingLogger()
    }

    @Test
    fun verbose_withoutThrowable() {
        val message = nextString()
        logger.verbose(message)
        logger.verifyLog(Verbose, message)
    }

    @Test
    fun verbose_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.verbose(message, throwable)
        logger.verifyLog(Verbose, message, throwable)
    }

    @Test
    fun debug_withoutThrowable() {
        val message = nextString()
        logger.debug(message)
        logger.verifyLog(Debug, message)
    }

    @Test
    fun debug_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.debug(message, throwable)
        logger.verifyLog(Debug, message, throwable)
    }

    @Test
    fun info_withoutThrowable() {
        val message = nextString()
        logger.info(message)
        logger.verifyLog(Info, message)
    }

    @Test
    fun info_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.info(message, throwable)
        logger.verifyLog(Info, message, throwable)
    }

    @Test
    fun warning_withoutThrowable() {
        val message = nextString()
        logger.warning(message)
        logger.verifyLog(Warning, message)
    }

    @Test
    fun warning_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.warning(message, throwable)
        logger.verifyLog(Warning, message, throwable)
    }

    @Test
    fun error_withoutThrowable() {
        val message = nextString()
        logger.error(message)
        logger.verifyLog(Error, message)
    }

    @Test
    fun error_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.error(message, throwable)
        logger.verifyLog(Error, message, throwable)
    }

    private fun CollectingLogger.verifyLog(level: Level, message: String, throwable: Throwable? = null) {
        assertEquals(LoggedMessage(level, message, throwable), logged.first())
    }
}
