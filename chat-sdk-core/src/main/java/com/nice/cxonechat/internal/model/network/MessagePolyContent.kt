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

package com.nice.cxonechat.internal.model.network

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
internal sealed class MessagePolyContent {

    @Serializable
    @SerialName("TEXT")
    data class Text(
        @SerialName("payload")
        val payload: Payload,
        @SerialName("fallback")
        val fallbackText: String? = null,
    ) : MessagePolyContent() {

        @Serializable
        data class Payload(
            @SerialName("text")
            val text: String,
        )
    }

    @Serializable
    @SerialName("QUICK_REPLIES")
    data class QuickReplies(
        @SerialName("fallbackText")
        val fallbackText: String,
        @SerialName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        @Serializable
        data class Payload(
            @SerialName("text")
            val text: WrappedText,
            @SerialName("actions")
            val actions: List<PolyAction>
        )
    }

    @Serializable
    @SerialName("LIST_PICKER")
    data class ListPicker(
        @SerialName("fallbackText")
        val fallbackText: String?,
        @SerialName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        @Serializable
        data class Payload(
            @SerialName("title")
            val title: WrappedText,
            @SerialName("text")
            val text: WrappedText,
            @SerialName("actions")
            val actions: List<PolyAction>
        )
    }

    @Serializable
    @SerialName("RICH_LINK")
    data class RichLink(
        @SerialName("fallbackText")
        val fallbackText: String,
        @SerialName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        @Serializable
        data class Payload(
            @SerialName("media")
            val media: MediaModel,
            @SerialName("title")
            val title: WrappedText,
            @SerialName("url")
            val url: String
        )
    }

    @Serializable
    data object Noop : MessagePolyContent()
}
