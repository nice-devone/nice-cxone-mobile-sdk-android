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

package com.nice.cxonechat.tool

import com.google.gson.FormattingStyle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.internal.Streams
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.NUMBER
import com.google.gson.stream.JsonWriter
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.util.timestampToDate
import com.nice.cxonechat.util.toTimestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import java.util.Date
import kotlin.math.roundToLong

internal inline fun <reified T : Any> T.serialize(): String = if (this::class.java.isAnnotationPresent(Serializable::class.java)) {
    Default.serializer.encodeToString(this)
} else {
    Gson.toJson(this)
}

internal val Gson = GsonBuilder()
    .registerTypeAdapter(Date::class.java, DateTypeAdapter())
    .registerTypeAdapterFactory(Factory())
    .setFormattingStyle(FormattingStyle.PRETTY)
    .create()

private class DateTypeAdapter : TypeAdapter<Date>() {

    override fun write(out: JsonWriter, value: Date?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.value(value.toTimestamp())
    }

    override fun read(reader: JsonReader): Date? {
        return when (reader.peek()) {
            NULL -> {
                reader.nextNull()
                null
            }

            NUMBER -> {
                var time = reader.nextDouble()
                if (time < epochLimitSeconds) time *= 1000
                Date(time.roundToLong())
            }

            else -> reader.nextString().timestampToDate()
        }
    }

    companion object {
        // this will make the program malfunction on Sat Nov 20 2286 17:46:40 UTC (:
        private const val epochLimitSeconds = 10_000_000_000L
    }
}


private class Factory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T> {
        val rawType: Class<in T> = type.rawType
        val annotation: Serializable? = rawType.getAnnotation(Serializable::class.java)

        val annotationPresent: Boolean = rawType.isAnnotationPresent(Serializable::class.java)

        if (annotation != null && annotationPresent) {
            return KotlinxAdapter(rawType)
        }

        return gson.getDelegateAdapter(this, type)
    }
}

private class KotlinxAdapter<T>(private val type: Class<in T>) : TypeAdapter<T>() {
    val serializer = Default.serializer.serializersModule.serializer(type)
    override fun write(out: JsonWriter, value: T?) {
        if (value == null) {
            out.nullValue()
            return
        }

        out.jsonValue(Default.serializer.encodeToString(serializer, value))
    }

    override fun read(reader: JsonReader): T? {
        return when (reader.peek()) {
            NULL -> {
                reader.nextNull()
                null
            }
            else -> Default.serializer.decodeFromString(serializer, Streams.parse(reader).toString()) as T
        }
    }
}
