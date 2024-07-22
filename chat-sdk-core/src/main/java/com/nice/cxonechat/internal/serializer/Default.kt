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

package com.nice.cxonechat.internal.serializer

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonSyntaxException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.NUMBER
import com.google.gson.stream.JsonWriter
import com.nice.cxonechat.internal.model.CustomFieldPolyType
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.PolyAction
import com.nice.cxonechat.util.DateTime
import com.nice.cxonechat.util.IsoDate
import com.nice.cxonechat.util.timestampToDate
import com.nice.cxonechat.util.toTimestamp
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

internal object Default {

    private val messageContentAdapter = RuntimeTypeAdapterFactory.of(MessagePolyContent::class.java, "type")
        .registerSubtype(MessagePolyContent.Text::class.java, "TEXT")
        .registerSubtype(MessagePolyContent.QuickReplies::class.java, "QUICK_REPLIES")
        .registerSubtype(MessagePolyContent.ListPicker::class.java, "LIST_PICKER")
        .registerSubtype(MessagePolyContent.RichLink::class.java, "RICH_LINK")
        .registerDefault(MessagePolyContent.Noop)
    private val customFieldTypeAdapter = RuntimeTypeAdapterFactory.of(CustomFieldPolyType::class.java, "type")
        .registerSubtype(CustomFieldPolyType.Text::class.java, "text")
        .registerSubtype(CustomFieldPolyType.Email::class.java, "email")
        .registerSubtype(CustomFieldPolyType.Selector::class.java, "list")
        .registerSubtype(CustomFieldPolyType.Hierarchy::class.java, "tree")
        .registerDefault(CustomFieldPolyType.Noop)
    private val actionAdapter = RuntimeTypeAdapterFactory.of(PolyAction::class.java, "type")
        .registerSubtype(PolyAction.ReplyButton::class.java, "REPLY_BUTTON")

    val serializer: Gson = GsonBuilder()
        .registerTypeAdapterFactory(messageContentAdapter)
        .registerTypeAdapterFactory(customFieldTypeAdapter)
        .registerTypeAdapterFactory(actionAdapter)
        .registerTypeAdapter(UUID::class.java, LenientUUIDTypeAdapter())
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .registerTypeAdapter(DateTime::class.java, DateTimeTypeAdapter())
        .registerTypeAdapter(IsoDate::class.java, IsoDateTypeAdapter())
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

    private class DateTimeTypeAdapter : TypeAdapter<DateTime>() {

        private val fallback = DateTypeAdapter()

        override fun write(out: JsonWriter, value: DateTime?) {
            if (value == null) {
                out.nullValue()
                return
            }
            out.value(value.toTimestamp())
        }

        override fun read(reader: JsonReader): DateTime? = fallback.read(reader)?.let(::DateTime)
    }

    private class IsoDateTypeAdapter : TypeAdapter<IsoDate>() {
        override fun write(out: JsonWriter?, value: IsoDate?) {
            if (value == null) {
                out?.nullValue()
            } else {
                out?.value(dateFormatter.format(value.date))
            }
        }

        override fun read(reader: JsonReader?): IsoDate? {
            return if (reader?.peek() == null) {
                return null
            } else {
                with(reader.nextString()) {
                    dateFormatter.parse(this) ?: throw JsonParseException("Unable to parse date:$this")
                }.let(::IsoDate)
            }
        }

        companion object {
            val dateFormatter by lazy {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            }
        }
    }

    /**
     * Modified version of default [type adapter][com.google.gson.internal.bind.TypeAdapters.UUID]
     * which deserializes empty strings as null values.
     */
    private class LenientUUIDTypeAdapter : TypeAdapter<UUID?>() {
        @Throws(IOException::class)
        @Suppress(
            "ReturnCount" // Suppressed for readability
        )
        override fun read(reader: JsonReader): UUID? {
            if (reader.peek() == NULL) {
                reader.nextNull()
                return null
            }
            val value = reader.nextString().ifEmpty {
                // Due to possible error on backend, the client can receive an empty string instead of null
                null
            } ?: return null
            try {
                return UUID.fromString(value)
            } catch (e: IllegalArgumentException) {
                throw JsonSyntaxException("Failed parsing '" + value + "' as UUID; at path " + reader.previousPath, e)
            }
        }

        @Throws(IOException::class)
        override fun write(writer: JsonWriter, value: UUID?) {
            writer.value(value?.toString())
        }
    }
}
