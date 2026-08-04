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

package com.nice.cxonechat.storage

import androidx.datastore.core.Serializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Cookie

internal class PlainCookieListSerializer : Serializer<List<Cookie>> {
    override val defaultValue: List<Cookie> = emptyList()
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(CookieAsStringSerializer)
    override suspend fun readFrom(input: java.io.InputStream): List<Cookie> {
        val bytes = input.readBytes()
        if (bytes.isEmpty()) return emptyList()
        return json.decodeFromString(listSerializer, bytes.decodeToString())
    }

    override suspend fun writeTo(t: List<Cookie>, output: java.io.OutputStream) {
        val encoded = json.encodeToString(listSerializer, t)
        output.write(encoded.toByteArray())
    }
}
