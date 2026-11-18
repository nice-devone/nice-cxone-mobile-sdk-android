/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.thread

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ContactHandlerTest {

    @Test
    fun `from maps cxone`() = assertEquals(ContactHandler.CXone, ContactHandler.from("cxone"))

    @Test
    fun `from maps cognigy`() = assertEquals(ContactHandler.Cognigy, ContactHandler.from("cognigy"))

    @Test
    fun `from ignores case`() = assertEquals(ContactHandler.Cognigy, ContactHandler.from("Cognigy"))

    @Test
    fun `from maps an unrecognised value to Unknown`() =
        assertEquals(ContactHandler.Unknown, ContactHandler.from("genesys"))

    @Test
    fun `from maps a missing value to null`() = assertNull(ContactHandler.from(null))
}
