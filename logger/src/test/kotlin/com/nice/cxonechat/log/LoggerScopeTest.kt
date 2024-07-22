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

import com.nice.cxonechat.log.fake.CollectingLogger
import com.nice.cxonechat.log.tool.nextString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class LoggerScopeTest {

    @Test
    fun testClassNamedScope() {
        val identity = CollectingLogger()
        val loggerScope = object : LoggerScope by LoggerScope<LoggerScopeTest>(identity) {}
        assertEquals(LoggerScopeTest::class.java.simpleName, loggerScope.scope)
        assertEquals(identity, loggerScope.identity)
        val level = Level.Info
        val message = nextString()
        loggerScope.log(level, message)
        val logged = identity.logged.first()
        assertEquals(level, logged.level)
        assertTrue {
            logged.message.contains(".*(${loggerScope.scope}).*($message)".toRegex())
        }
    }

    @Test
    fun testCustomNamedScope() {
        val customName = nextString()
        val identity = CollectingLogger()
        val loggerScope = object : LoggerScope by LoggerScope(customName, identity) {}
        assertEquals(customName, loggerScope.scope)
        assertEquals(identity, loggerScope.identity)
        val level = Level.Debug
        val message = nextString()
        loggerScope.log(level, message)
        val logged = identity.logged.first()
        assertEquals(level, logged.level)
        assertTrue {
            logged.message.contains(".*(${loggerScope.scope}).*($message)".toRegex())
        }
    }

    @Test
    fun testSubScope() {
        val scopeName = nextString()
        val subScopeName = nextString()
        val identity = CollectingLogger()
        val level = Level.Verbose
        val message = nextString()
        val loggerScope = object : LoggerScope by LoggerScope(scopeName, identity) {
            fun subScope() = scope(subScopeName) {
                assertTrue("Expected that the '$scope' will contain '$scopeName' followed by '$subScopeName'") {
                    scope.contains(".*($scopeName).*($subScopeName)".toRegex())
                }
                assertEquals(identity, this.identity)
                log(level, message)
            }
        }
        loggerScope.subScope()
    }
}
