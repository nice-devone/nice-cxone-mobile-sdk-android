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

package com.nice.cxonechat.storage

import com.nice.cxonechat.tool.getPublicProperties
import com.nice.cxonechat.tool.nextString
import org.junit.Before
import org.junit.Test
import kotlin.reflect.KMutableProperty
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * This test class aims to cover if the [PreferencesValueStorage] implementation is fulfilling requirements defined in
 * the [ValueStorage] interface.
 */
internal class PreferencesValueStorageContractTest {

    lateinit var storage: ValueStorage

    @Before
    fun setup() {
        storage = PreferencesValueStorage(FakeSharedPreferences())
    }

    @Test
    fun authTokenDefaultValueIsNull() {
        assertNull(storage.authToken)
    }

    @Test
    fun authTokenExpDateDefaultIsEmpty() {
        assertNull(storage.authTokenExpDate)
    }

    @Test
    fun consumerIdDefaultValueIsEmpty() {
        assertNull(storage.customerId)
    }

    @Test
    fun welcomeMessageDefaultIsEmpty() {
        assertEquals("", storage.welcomeMessage)
    }

    @Test
    fun storageIsStoringValues() {
        val properties = storage::class.members.getPublicProperties()
        properties.fillStorageWithRandomValues()
            .forEach { (property, setValue) ->
                assertEquals(setValue, getStoredProperty(property))
            }
    }

    @Test
    fun testClearStorage() {
        val properties = storage::class.members.getPublicProperties()
        val propertiesAndSetValues = properties.fillStorageWithRandomValues()
        storage.clearStorage()
        propertiesAndSetValues.forEach { (property, setValue) ->
            assertNotEquals(setValue, getStoredProperty(property))
        }
    }

    private fun getStoredProperty(property: KMutableProperty<*>) = property.getter.call(storage)

    private fun List<KMutableProperty<*>>.fillStorageWithRandomValues(): Map<KMutableProperty<*>, Any> = mapNotNull { property ->
        when (property.returnType.classifier) {
            String::class -> property to nextString().also { property.setter.call(storage, it) }
            else -> null // unimplemented
        }
    }.toMap()
}
