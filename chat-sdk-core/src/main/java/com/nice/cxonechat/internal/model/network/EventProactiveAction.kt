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

import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.analytics.ActionMetadataInternal
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

@Serializable
internal data class EventProactiveAction(
    @SerialName("data")
    val data: Data,
) {

    val type get() = data.proactiveAction.action.actionType
    val metadata get() = data.proactiveAction.action.toActionMetadata()
    val customFields get() = data.proactiveAction.action.data.handover.customFields.orEmpty()
    val bodyText get() = data.proactiveAction.action.data.content.bodyText
    val headlineText get() = data.proactiveAction.action.data.content.headlineText
    val headlineSecondaryText get() = data.proactiveAction.action.data.content.headlineSecondaryText
    val image get() = data.proactiveAction.action.data.content.image
    val mimeType get() = data.proactiveAction.action.data.content.mimeType
    val variables get() = data.proactiveAction.action.data.content.variables.orEmpty().mapValues { entry ->
        when(val element = entry.value) {
            null -> null
            is JsonPrimitive -> element.content
            else -> Default.serializer.encodeToString(element)
        }
    }

    @Serializable
    data class Data(
        @SerialName("proactiveAction")
        val proactiveAction: Action,
    )

    @Serializable
    data class Action(
        @SerialName("action")
        val action: ProactiveActionDetails,
    )

    @Serializable
    data class ProactiveActionDetails(
        @SerialName("actionId")
        @Contextual
        val actionId: UUID,
        @SerialName("actionName")
        val actionName: String,
        @SerialName("actionType")
        val actionType: ActionType,
        @SerialName("data")
        val data: ActionData,
    ) {

        fun toActionMetadata(): ActionMetadata = ActionMetadataInternal(
            id = actionId,
            name = actionName,
            type = actionType
        )
    }

    @Serializable
    data class ActionData(
        @SerialName("content")
        val content: Content,
        @SerialName("handover")
        val handover: Handover,
    )

    @Serializable
    data class Content(
        @SerialName("bodyText")
        val bodyText: String,
        @SerialName("headlineText")
        val headlineText: String? = null,
        @SerialName("headlineSecondaryText")
        val headlineSecondaryText: String? = null,
        @SerialName("image")
        val image: String? = null,
        @SerialName("mimeType")
        val mimeType: String? = null,
        @SerialName("variables")
        val variables: Map<String, JsonElement?>? = null,
    )

    @Serializable
    data class Handover(
        @SerialName("customFields")
        val customFields: List<CustomFieldModel>? = null,
    )

    companion object : ReceivedEvent<EventProactiveAction> {
        override val type = EventType.FireProactiveAction
    }
}
