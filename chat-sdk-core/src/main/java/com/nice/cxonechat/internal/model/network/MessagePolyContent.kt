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

import com.nice.cxonechat.util.IsoDate
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
        @SerialName("parameters")
        val parameters: Parameters? = null,
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
        val payload: Payload,
    ) : MessagePolyContent() {
        @Serializable
        data class Payload(
            @SerialName("text")
            val text: WrappedText,
            @SerialName("actions")
            val actions: List<PolyAction>,
        )
    }

    @Serializable
    @SerialName("LIST_PICKER")
    data class ListPicker(
        @SerialName("fallbackText")
        val fallbackText: String?,
        @SerialName("payload")
        val payload: Payload,
    ) : MessagePolyContent() {
        @Serializable
        data class Payload(
            @SerialName("title")
            val title: WrappedText,
            @SerialName("text")
            val text: WrappedText,
            @SerialName("actions")
            val actions: List<PolyAction>,
        )
    }

    @Serializable
    @SerialName("RICH_LINK")
    data class RichLink(
        @SerialName("fallbackText")
        val fallbackText: String,
        @SerialName("payload")
        val payload: Payload,
    ) : MessagePolyContent() {
        @Serializable
        data class Payload(
            @SerialName("media")
            val media: MediaModel,
            @SerialName("title")
            val title: WrappedText,
            @SerialName("url")
            val url: String,
        )
    }

    @Serializable
    @SerialName(Plugin.TYPE)
    data class Plugin(
        @SerialName("fallbackText")
        val fallbackText: String,
        @SerialName("payload")
        val payload: Payload? = null,
    ) : MessagePolyContent() {
        companion object {
            const val TYPE = "PLUGIN"
        }

        @Serializable
        data class Payload(
            @SerialName("postback")
            val postback: String? = null,
            @SerialName("elements")
            val elements: List<PluginElement>? = null,
        )

        @Serializable
        @JsonClassDiscriminator("type")
        internal sealed interface PluginElement {
            sealed interface SimpleElement : PluginElement {
                @Serializable
                @SerialName("TEXT")
                data class TextElement(
                    @SerialName("text")
                    val text: String,
                ) : SimpleElement

                @Serializable
                @SerialName("TITLE")
                data class TitleElement(
                    @SerialName("text")
                    val text: String,
                ) : SimpleElement

                @Serializable
                @SerialName("COUNTDOWN")
                data class CounterElement(
                    @SerialName("variables")
                    val variables: Variables,
                ) : SimpleElement {
                    @Serializable
                    data class Variables(
                        @SerialName("startedAt")
                        val startedAt: IsoDate,
                        @SerialName("numberOfSeconds")
                        val numberOfSeconds: Long,
                    )
                }

                @Serializable
                @SerialName("BUTTON")
                data class ButtonElement(
                    @SerialName("text")
                    val text: String,
                    @SerialName("postback")
                    val postback: String? = null,
                ) : SimpleElement

                @Serializable
                data class Unsupported(
                    @SerialName("type")
                    val type: String,
                ) : SimpleElement

                @Serializable
                data object Noop : SimpleElement
            }

            sealed interface StructuredElements : PluginElement {
                @Serializable
                @SerialName(InactivityPlugin.TYPE)
                data class InactivityPlugin(
                    @SerialName("elements")
                    val elements: List<SimpleElement>,
                ) : StructuredElements {
                    companion object {
                        const val TYPE = "INACTIVITY_POPUP"
                    }
                }
            }
        }
    }

    /**
     * A message content type for postback actions, which can be used to trigger specific actions in the application or
     * backend.
     */
    @Serializable
    @SerialName("POSTBACK")
    data class Postback(
        @SerialName("postback")
        val postback: String?,
        @SerialName("payload")
        val payload: Payload? = null,
    ) : MessagePolyContent() {
        @Serializable
        data class Payload(
            @SerialName("text")
            val text: String? = null,
            @SerialName("postback")
            val postback: String? = null,
        )
    }

    /**
     * A generic fallback message type for messages that are not supported by the current version of the SDK.
     * Note for testing: This class can't be serialized using the
     * [com.nice.cxonechat.internal.serializer.Default.serializer] as it specified `type` field which is in conflict
     * with the discriminator specified for the default serializer polymorphic serialization.
     */
    @Serializable
    data class Unsupported(
        @SerialName("type")
        val type: String,
        @SerialName("fallbackText")
        val fallbackText: String,
        @SerialName("payload")
        val payload: Payload? = null,
    ) : MessagePolyContent() {
        val specificType: String?
            get() = payload?.elements?.firstOrNull()?.type

        @Serializable
        data class Payload(
            @SerialName("elements")
            val elements: List<SubElement>? = null,
        )

        @Serializable
        data class SubElement(@SerialName("type") val type: String)
    }

    /**
     * A no-operation message content type, used when the message content is unparsable and will be ignored.
     */
    @Serializable
    data object Noop : MessagePolyContent()
}
