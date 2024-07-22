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

import com.nice.cxonechat.log.Level.Custom
import com.nice.cxonechat.log.Level.Verbose
import com.nice.cxonechat.log.fake.CollectingLogger
import com.nice.cxonechat.log.fake.LoggedMessage
import com.nice.cxonechat.log.tool.nextString
import org.junit.Test
import kotlin.test.assertEquals

internal class ProxyLoggerTest {
    @Test
    fun testAdd() {
        val logger = ProxyLogger()
        assertEquals(0, logger.loggerCount)
        val testLogger1 = CollectingLogger()
        val testLogger2 = CollectingLogger()
        logger.add(testLogger1, testLogger2)
        assertEquals(2, logger.loggerCount)
        assertEquals(listOf(testLogger1, testLogger2), logger.loggers)
    }

    @Test
    fun addAll() {
        val logger = ProxyLogger()
        assertEquals(0, logger.loggerCount)
        val list = listOf(CollectingLogger(), CollectingLogger())
        logger.addAll(list)
        assertEquals(2, logger.loggerCount)
        assertEquals(list, logger.loggers)
    }

    @Test
    fun remove() {
        val logger = ProxyLogger()
        assertEquals(0, logger.loggerCount)
        val testLogger = CollectingLogger()
        val list = listOf(testLogger, CollectingLogger())
        logger.addAll(list)
        logger.remove(testLogger)
        assertEquals(1, logger.loggerCount)
        assertEquals(list - testLogger, logger.loggers)
    }

    @Test
    fun clear() {
        val logger = ProxyLogger()
        assertEquals(0, logger.loggerCount)
        val list = listOf(CollectingLogger(), CollectingLogger())
        logger.addAll(list)
        logger.clear()
        assertEquals(0, logger.loggerCount)
        assertEquals(emptyList(), logger.loggers)
    }

    @Test
    fun addAndGetLoggerCount() {
        val logger = ProxyLogger()
        assertEquals(0, logger.loggerCount)
        logger.add(CollectingLogger())
        assertEquals(1, logger.loggerCount)
    }

    @Test
    fun log() {
        val logger = ProxyLogger()
        assertEquals(0, logger.loggerCount)
        val list = listOf(CollectingLogger(), CollectingLogger())
        logger.addAll(list)
        val toLogList: List<LoggedMessage> = listOf(
            LoggedMessage(Custom(1), nextString(), IllegalStateException(nextString())),
            LoggedMessage(Verbose, nextString(), null)
        )
        toLogList.forEach { toLog ->
            logger.log(
                level = toLog.level,
                message = toLog.message,
                throwable = toLog.throwable
            )
        }
        list.forEach { proxiedLogger ->
            assertEquals(toLogList, proxiedLogger.logged)
        }
    }
}
