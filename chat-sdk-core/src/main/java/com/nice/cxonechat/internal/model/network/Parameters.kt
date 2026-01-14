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

package com.nice.cxonechat.internal.model.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable(with = ParametersSerializer::class)
internal sealed class Parameters {
    @Serializable
    data class Object(
        val isInitialMessage: Boolean? = null,
        val isUnsupportedMessageTypeAnswer: Boolean? = null, // For unsupported messages
    ) : Parameters()
    data object Array : Parameters()
}

internal object ParametersSerializer : KSerializer<Parameters> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Parameters")

    override fun deserialize(decoder: Decoder): Parameters {
        val input = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        return when (val element = input.decodeJsonElement()) {
            is JsonObject -> {
                val isInitialMessage = element["isInitialMessage"]?.jsonPrimitive?.booleanOrNull
                val isUnsupportedMessageTypeAnswer = element["isUnsupportedMessageTypeAnswer"]?.jsonPrimitive?.booleanOrNull
                Parameters.Object(isInitialMessage, isUnsupportedMessageTypeAnswer)
            }

            is JsonArray -> Parameters.Array
            else -> Parameters.Array
        }
    }

    override fun serialize(encoder: Encoder, value: Parameters) {
        val output = encoder as? JsonEncoder ?: error("Can be serialized only by JSON")
        when (value) {
            is Parameters.Object -> {
                val obj = buildJsonObject {
                    value.isInitialMessage?.let { put("isInitialMessage", it) }
                    value.isUnsupportedMessageTypeAnswer?.let { put("isUnsupportedMessageTypeAnswer", it) }
                }
                output.encodeJsonElement(obj)
            }

            is Parameters.Array -> output.encodeJsonElement(JsonArray(emptyList()))
        }
    }
}
