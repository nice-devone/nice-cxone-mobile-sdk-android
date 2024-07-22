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

import com.nice.cxonechat.log.Level.Info
import com.nice.cxonechat.log.Level.Verbose
import com.nice.cxonechat.log.fake.CollectingLogger
import com.nice.cxonechat.log.fake.LoggedMessage
import com.nice.cxonechat.log.tool.nextString
import org.junit.Test
import kotlin.reflect.KFunction2
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class LoggerExtTest {
    @Test
    fun test_verbose() {
        val logger = CollectingLogger()
        val level = Verbose
        val methodToTest = logger::verbose
        testMethod(logger, level, methodToTest)
    }

    @Test
    fun test_debug() {
        val logger = CollectingLogger()
        val level = Level.Debug
        val methodToTest = logger::debug
        testMethod(logger, level, methodToTest)
    }

    @Test
    fun test_info() {
        val logger = CollectingLogger()
        val level = Info
        val methodToTest = logger::info
        testMethod(logger, level, methodToTest)
    }

    @Test
    fun test_warning() {
        val logger = CollectingLogger()
        val level = Level.Warning
        val methodToTest = logger::warning
        testMethod(logger, level, methodToTest)
    }

    @Test
    fun test_error() {
        val logger = CollectingLogger()
        val level = Level.Error
        val methodToTest = logger::error
        testMethod(logger, level, methodToTest)
    }

    @Test
    fun test_duration() {
        val logger = CollectingLogger()
        val expectedResult = 42L
        val expectedMessages: List<LoggedMessage> = listOf(
            LoggedMessage(Verbose, "Started", null),
            LoggedMessage(Info, "Going to sleep", null),
            LoggedMessage(Info, "Awake", null),
            LoggedMessage(Verbose, "Finished", null),
        )
        val result = logger.duration {
            logger.log(expectedMessages[1].level, expectedMessages[1].message)
            Thread.sleep(expectedResult)
            logger.log(expectedMessages[2].level, expectedMessages[2].message)
            expectedResult
        }
        assertEquals(expectedResult, result)
        assertEquals(expectedMessages.size, logger.logged.size)
        expectedMessages.forEachIndexed { index, triple ->
            val (expectedLevel, expectedMessage, _) = triple
            val (level, message, _) = logger.logged[index]
            assertEquals(expectedLevel, level)
            assertTrue(message.startsWith(expectedMessage))
        }
    }

    private fun testMethod(logger: CollectingLogger, level: Level, methodToTest: KFunction2<String, Throwable?, Unit>) {
        val listOfExpected: List<LoggedMessage> = listOf(
            LoggedMessage(level, nextString(), RuntimeException(nextString())),
            LoggedMessage(level, nextString(), null)
        )
        listOfExpected.forEach { (_, message, throwable) -> methodToTest(message, throwable) }
        assertEquals(listOfExpected, logger.logged)
    }
}
