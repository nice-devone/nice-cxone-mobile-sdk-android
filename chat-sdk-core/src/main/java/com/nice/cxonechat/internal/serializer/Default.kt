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

import com.nice.cxonechat.core.BuildConfig
import com.nice.cxonechat.internal.model.CustomFieldPolyType
import com.nice.cxonechat.internal.model.ErrorModel
import com.nice.cxonechat.internal.model.network.EventMessageReadByAgent
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.PolyAction
import com.nice.cxonechat.internal.serializer.Default.DateAsNumberSerializer
import com.nice.cxonechat.internal.serializer.Default.DateSerializer
import com.nice.cxonechat.internal.serializer.Default.UUIDSerializer
import com.nice.cxonechat.util.DateTime
import com.nice.cxonechat.util.IsoDate
import com.nice.cxonechat.util.timestampToDate
import com.nice.cxonechat.util.toTimestamp
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.ClassDiscriminatorMode.POLYMORPHIC
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

internal object Default {

    private val webSocketModule = SerializersModule {
        polymorphic(Any::class) {
            subclass(ErrorModel::class)
            subclass(EventMessageReadByAgent::class)
        }
    }

    private val messageContentModule = SerializersModule {
        polymorphic(MessagePolyContent::class) {
            subclass(MessagePolyContent.Text::class)
            subclass(MessagePolyContent.QuickReplies::class)
            subclass(MessagePolyContent.ListPicker::class)
            subclass(MessagePolyContent.RichLink::class)
            defaultDeserializer { MessagePolyContent.Noop.serializer() }
        }
    }

    private val customFieldTypeModule = SerializersModule {
        polymorphic(CustomFieldPolyType::class) {
            subclass(CustomFieldPolyType.Text::class)
            subclass(CustomFieldPolyType.Email::class)
            subclass(CustomFieldPolyType.Selector::class)
            subclass(CustomFieldPolyType.Hierarchy::class)
            defaultDeserializer { CustomFieldPolyType.Noop.serializer() }
        }
    }
    private val actionModule = SerializersModule {
        polymorphic(PolyAction::class) {
            subclass(PolyAction.ReplyButton::class)
        }
    }

    private val contextualModule = SerializersModule {
        contextual(DateSerializer)
        contextual(DateTimeSerializer)
        contextual(IsoDateSerializer)
        contextual(UUIDSerializer as KSerializer<UUID>)
    }

    @OptIn(ExperimentalSerializationApi::class)
    val serializer: Json = Json {
        serializersModule = webSocketModule +
                messageContentModule +
                customFieldTypeModule +
                actionModule +
                contextualModule
        encodeDefaults = true // We are prefilling some constant values for serialization
        ignoreUnknownKeys = true // We are ignoring unused properties
        isLenient = false // Default is false
        coerceInputValues = true
        explicitNulls = false // Backend omits null values
        prettyPrint = BuildConfig.DEBUG
        classDiscriminatorMode = POLYMORPHIC // Mostly implicit
    }

    internal object DateSerializer : KSerializer<Date> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateSerializer", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(value.toTimestamp())
        override fun deserialize(decoder: Decoder): Date = decoder.decodeString().timestampToDate()
    }

    internal object DateTimeSerializer : KSerializer<DateTime> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateTimeSerializer", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: DateTime) = encoder.encodeString(value.toTimestamp())
        override fun deserialize(decoder: Decoder): DateTime = decoder.decodeSerializableValue(DateSerializer).let(::DateTime)
    }

    internal object DateAsNumberSerializer : KSerializer<Date> {
        // this will make the program malfunction on Sat Nov 20 2286 17:46:40 UTC (:
        private const val epochLimitSeconds = 10_000_000_000L
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateAsNumberSerializer", PrimitiveKind.LONG)
        override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
        override fun deserialize(decoder: Decoder): Date {
            var time = decoder.decodeLong().toDouble()
            if (time < epochLimitSeconds) time *= 1000
            return Date(time.roundToLong())
        }
    }

    internal object IsoDateSerializer : KSerializer<IsoDate> {
        val dateFormatter: SimpleDateFormat
            get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IsoDateSerializer", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: IsoDate) = encoder.encodeString(dateFormatter.format(value.date))
        override fun deserialize(decoder: Decoder): IsoDate {
            val date = dateFormatter.parse(decoder.decodeString()) ?: throw IsoDateSerializationException()
            return IsoDate(
                date
            )
        }

        private class IsoDateSerializationException : SerializationException("Invalid date format")
    }

    internal object UUIDSerializer : KSerializer<UUID?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUIDSerializer", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: UUID?) {
            value.toString().apply(encoder::encodeString)
        }

        override fun deserialize(decoder: Decoder): UUID? {
            require(decoder is JsonDecoder)
            val element = decoder.decodeJsonElement()

            val out = if (element is JsonPrimitive && element.isString) {
                val content = element.content
                if (content.isNotEmpty()) {
                    runCatching { UUID.fromString(content) }.getOrNull()
                } else {
                    null
                }
            } else {
                null
            }
            return out
        }
    }
}

internal typealias DateAsString =
        @Serializable(DateSerializer::class)
        Date

internal typealias DateAsNumber =
        @Serializable(DateAsNumberSerializer::class)
        Date

internal typealias SerializedUUID =
        @Serializable(UUIDSerializer::class)
        UUID
