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
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.analytics.ActionMetadataInternal
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import java.util.UUID

internal data class EventProactiveAction(
    @SerializedName("data")
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
    val variables get() = data.proactiveAction.action.data.content.variables

    data class Data(
        @SerializedName("proactiveAction")
        val proactiveAction: Action,
    )

    data class Action(
        @SerializedName("action")
        val action: ProactiveActionDetails,
    )

    data class ProactiveActionDetails constructor(
        @SerializedName("actionId")
        val actionId: UUID,
        @SerializedName("actionName")
        val actionName: String,
        @SerializedName("actionType")
        val actionType: ActionType,
        @SerializedName("data")
        val data: ActionData,
    ) {

        fun toActionMetadata(): ActionMetadata = ActionMetadataInternal(
            id = actionId,
            name = actionName,
            type = actionType
        )
    }

    data class ActionData(
        @SerializedName("content")
        val content: Content,
        @SerializedName("handover")
        val handover: Handover,
    )

    data class Content(
        @SerializedName("bodyText")
        val bodyText: String,
        @SerializedName("headlineText")
        val headlineText: String? = null,
        @SerializedName("headlineSecondaryText")
        val headlineSecondaryText: String? = null,
        @SerializedName("image")
        val image: String? = null,
        @SerializedName("mimeType")
        val mimeType: String? = null,
        @SerializedName("variables")
        val variables: Map<String, Any?>? = null,
    )

    data class Handover(
        @SerializedName("customFields")
        val customFields: List<CustomFieldModel>? = null,
    )

    companion object : ReceivedEvent<EventProactiveAction> {
        override val type = EventType.FireProactiveAction
    }
}
