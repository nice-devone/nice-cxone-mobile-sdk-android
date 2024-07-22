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
import com.nice.cxonechat.enums.MessageContentType
import com.nice.cxonechat.enums.MessageContentType.Text

internal data class MessageContent(
    /**
     * This message's type. It can have various types on
     * which [MessagePayload] content depends.
     *
     * @see MessageContentType
     * */
    @SerializedName("type")
    val type: MessageContentType,

    /**
     * Message contents sent to the remote agent/server.
     * It can have various elements depending on [MessageContentType]
     * and its supported parameters.
     *
     * @see MessageContentType
     * */
    @SerializedName("payload")
    val payload: MessagePayload,

    /**
     * Postback string value.
     * Supplied by the integrating application, for messages which signifies reaction
     * to an [com.nice.cxonechat.message.Action] by the customer.
     *
     * For an example, when user clicks a reply button from a rich message, the application
     * should then send a message containing original [com.nice.cxonechat.message.Action.ReplyButton.postback],
     * so an automatic backend process can react to that selection.
     */
    @SerializedName("postback")
    val postback: String? = null,
) {

    constructor(
        message: String,
        postback: String? = null,
    ) : this(
        type = Text,
        payload = MessagePayload(text = message),
        postback = postback
    )
}
