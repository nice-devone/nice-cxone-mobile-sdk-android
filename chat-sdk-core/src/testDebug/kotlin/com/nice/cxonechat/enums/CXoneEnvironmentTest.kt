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

package com.nice.cxonechat.enums

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CXoneEnvironmentTest {

    @Test
    fun `all entries have non-blank tokenUrl`() {
        CXoneEnvironment.entries.forEach { env ->
            assertTrue(
                env.value.tokenUrl.isNotBlank(),
                "tokenUrl should not be blank for ${env.name}"
            )
        }
    }

    @Test
    fun `all tokenUrls use https scheme`() {
        CXoneEnvironment.entries.forEach { env ->
            assertTrue(
                env.value.tokenUrl.startsWith("https://"),
                "tokenUrl for ${env.name} should start with https://, got: ${env.value.tokenUrl}"
            )
        }
    }

    @Test
    fun `all tokenUrls end with a trailing slash`() {
        CXoneEnvironment.entries.forEach { env ->
            assertTrue(
                env.value.tokenUrl.endsWith("/"),
                "tokenUrl for ${env.name} should end with /, got: ${env.value.tokenUrl}"
            )
        }
    }

    @Test
    fun `all tokenUrls are unique`() {
        val urls = CXoneEnvironment.entries.map { it.value.tokenUrl }
        assertEquals(
            urls.toSet().size,
            urls.size,
            "Every CXoneEnvironment entry must have a distinct tokenUrl"
        )
    }

    @Test
    fun `all entries have non-blank baseUrl`() {
        CXoneEnvironment.entries.forEach { env ->
            assertTrue(
                env.value.baseUrl.isNotBlank(),
                "baseUrl should not be blank for ${env.name}"
            )
        }
    }

    @Test
    fun `all entries have non-blank socketUrl`() {
        CXoneEnvironment.entries.forEach { env ->
            assertTrue(
                env.value.socketUrl.isNotBlank(),
                "socketUrl should not be blank for ${env.name}"
            )
        }
    }

    @Test
    fun `toString contains tokenUrl field label`() {
        CXoneEnvironment.entries.forEach { env ->
            val str = env.value.toString()
            assertTrue(
                str.contains("tokenUrl="),
                "toString() for ${env.name} should contain 'tokenUrl=', got: $str"
            )
            assertFalse(
                str.contains("authUrl="),
                "toString() for ${env.name} must not contain deprecated 'authUrl=', got: $str"
            )
        }
    }

    @Test
    fun `NA1 tokenUrl points to expected OAuth host`() {
        assertTrue(
            CXoneEnvironment.NA1.value.tokenUrl.contains("digital-oauth-de-na1"),
            "NA1 tokenUrl should reference the na1 OAuth host"
        )
    }

    @Test
    fun `EU1 tokenUrl points to expected OAuth host`() {
        assertTrue(
            CXoneEnvironment.EU1.value.tokenUrl.contains("digital-oauth-de-eu1"),
            "EU1 tokenUrl should reference the eu1 OAuth host"
        )
    }
}
