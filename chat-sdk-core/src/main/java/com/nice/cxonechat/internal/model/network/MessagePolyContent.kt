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

import com.google.gson.annotations.SerializedName

internal sealed class MessagePolyContent {

    data class Text(
        @SerializedName("payload")
        val payload: Payload,
        @SerializedName("fallback")
        val fallbackText: String? = null,
    ) : MessagePolyContent() {

        data class Payload(
            @SerializedName("text")
            val text: String,
        )
    }

    data class QuickReplies(
        @SerializedName("fallbackText")
        val fallbackText: String,
        @SerializedName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        data class Payload(
            @SerializedName("text")
            val text: WrappedText,
            @SerializedName("actions")
            val actions: List<PolyAction>
        )
    }

    data class ListPicker(
        @SerializedName("fallbackText")
        val fallbackText: String?,
        @SerializedName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        data class Payload(
            @SerializedName("title")
            val title: WrappedText,
            @SerializedName("text")
            val text: WrappedText,
            @SerializedName("actions")
            val actions: List<PolyAction>
        )
    }

    data class RichLink(
        @SerializedName("fallbackText")
        val fallbackText: String,
        @SerializedName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        data class Payload(
            @SerializedName("media")
            val media: MediaModel,
            @SerializedName("title")
            val title: WrappedText,
            @SerializedName("url")
            val url: String
        )
    }

    object Noop : MessagePolyContent()
}
