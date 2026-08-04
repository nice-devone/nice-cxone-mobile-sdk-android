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

package com.nice.cxonechat.internal.serializer

import com.nice.cxonechat.core.BuildConfig
import com.nice.cxonechat.internal.model.CustomFieldPolyType
import com.nice.cxonechat.internal.model.ErrorModel
import com.nice.cxonechat.internal.model.network.EventMessageReadByAgent
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Plugin.PluginElement
import com.nice.cxonechat.internal.model.network.PolyAction
import com.nice.cxonechat.internal.serializer.Default.InstantAsNumberSerializer
import com.nice.cxonechat.internal.serializer.Default.InstantSerializer
import com.nice.cxonechat.internal.serializer.Default.UUIDSerializer
import com.nice.cxonechat.util.toInstant
import com.nice.cxonechat.util.toTimestamp
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.ClassDiscriminatorMode.POLYMORPHIC
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.util.UUID
import kotlin.time.Instant

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
            subclass(MessagePolyContent.TimePicker::class)
            subclass(MessagePolyContent.Plugin::class)
            defaultDeserializer { DefaultMessagePolyContentSerializer }
        }
    }

    private val pluginElementModule = SerializersModule {
        polymorphic(PluginElement::class) {
            subclass(PluginElement.StructuredElements.InactivityPlugin::class)
            subclass(PluginElement.SimpleElement.TitleElement::class)
            subclass(PluginElement.SimpleElement.TextElement::class)
            subclass(PluginElement.SimpleElement.ButtonElement::class)
            subclass(PluginElement.SimpleElement.CounterElement::class)
            defaultDeserializer { DefaultElementPolyContentSerializer }
        }
    }
    private val pluginSimpleElementModule = SerializersModule {
        polymorphic(PluginElement.SimpleElement::class) {
            subclass(PluginElement.SimpleElement.TitleElement::class)
            subclass(PluginElement.SimpleElement.TextElement::class)
            subclass(PluginElement.SimpleElement.ButtonElement::class)
            subclass(PluginElement.SimpleElement.CounterElement::class)
            defaultDeserializer { DefaultSimpleElementPolyContentSerializer }
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
        contextual(InstantSerializer)
        @Suppress("UNCHECKED_CAST")
        contextual(UUIDSerializer as KSerializer<UUID>)
    }

    @OptIn(ExperimentalSerializationApi::class)
    val serializer: Json = Json {
        serializersModule = webSocketModule +
                messageContentModule +
                pluginElementModule +
                pluginSimpleElementModule +
                customFieldTypeModule +
                actionModule +
                contextualModule
        encodeDefaults = true // We are prefilling some constant values for serialization
        ignoreUnknownKeys = true // We are ignoring unused properties
        isLenient = false // Default is false
        coerceInputValues = true
        explicitNulls = false // Backend omits null values
        @Suppress("KotlinConstantConditions") // BuildConfig.DEBUG is a build-time generated constant
        prettyPrint = BuildConfig.DEBUG
        classDiscriminatorMode = POLYMORPHIC // Mostly implicit
        exceptionsWithDebugInfo = BuildConfig.DEBUG // Prevent PII leakage in exceptions logged to remote logger in production
    }

    /**
     * Serializes [Instant] as an ISO 8601 string.
     *
     * Deserialization handles both standard ISO 8601 (uppercase Z or numeric offset) and the legacy
     * backend format that uses a literal lowercase 'z' suffix for UTC.
     *
     * Serialization emits the format produced by [kotlin.time.Instant.toString], truncated to
     * millisecond precision (e.g. `"2024-01-01T12:00:00.123Z"`). Fractional seconds are omitted
     * when the millisecond component is zero (e.g. `"2024-01-01T12:00:00Z"`).
     */
    internal object InstantSerializer : KSerializer<Instant> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toTimestamp())

        override fun deserialize(decoder: Decoder): Instant = decoder.decodeString().toInstant()
    }

    /**
     * Serializes [Instant] as an epoch number.
     *
     * Deserialization accepts both epoch-second values (< 10^10) and epoch-millisecond values,
     * matching the original behaviour. This will malfunction on 2286-11-20 when epoch seconds
     * exceed the threshold.
     */
    internal object InstantAsNumberSerializer : KSerializer<Instant> {
        private const val EPOCH_LIMIT_SECONDS = 10_000_000_000L

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantAsNumberSerializer", PrimitiveKind.LONG)

        override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilliseconds())

        override fun deserialize(decoder: Decoder): Instant {
            val epochRaw = decoder.decodeLong()
            val epochMs = if (epochRaw < EPOCH_LIMIT_SECONDS) epochRaw * 1000L else epochRaw
            return Instant.fromEpochMilliseconds(epochMs)
        }
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

    /**
     * Fallback deserializer for [MessagePolyContent], it checks if the received json contains fields required for parsing
     * of [MessagePolyContent.Unsupported], if the fields are missing it will use [MessagePolyContent.Noop].
     */
    internal object DefaultMessagePolyContentSerializer :
        JsonContentPolymorphicSerializer<MessagePolyContent>(MessagePolyContent::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<MessagePolyContent> = when {
            "type" in element.jsonObject && "fallbackText" in element.jsonObject -> MessagePolyContent.Unsupported.serializer()
            else -> MessagePolyContent.Noop.serializer()
        }
    }

    internal object DefaultElementPolyContentSerializer :
        JsonContentPolymorphicSerializer<PluginElement>(PluginElement::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PluginElement> = when {
            "type" in element.jsonObject -> PluginElement.SimpleElement.Unsupported.serializer()
            else -> PluginElement.SimpleElement.Noop.serializer()
        }
    }

    internal object DefaultSimpleElementPolyContentSerializer :
        JsonContentPolymorphicSerializer<PluginElement.SimpleElement>(PluginElement.SimpleElement::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<PluginElement.SimpleElement> = when {
            "type" in element.jsonObject -> PluginElement.SimpleElement.Unsupported.serializer()
            else -> PluginElement.SimpleElement.Noop.serializer()
        }
    }
}

internal typealias InstantAsString =
        @Serializable(InstantSerializer::class)
        Instant

internal typealias InstantAsNumber =
        @Serializable(InstantAsNumberSerializer::class)
        Instant

internal typealias SerializedUUID =
        @Serializable(UUIDSerializer::class)
        UUID
