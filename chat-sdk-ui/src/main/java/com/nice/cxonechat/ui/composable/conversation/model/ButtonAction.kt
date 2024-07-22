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

package com.nice.cxonechat.ui.composable.conversation.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nice.cxonechat.message.OutboundMessage

internal typealias ButtonAction = (Context) -> Unit

internal fun compoundAction(
    vararg actions: ButtonAction?
): ButtonAction? {
    val todo = actions.filterNotNull()

    return when {
        todo.isEmpty() -> null
        todo.size == 1 -> todo.first()
        else -> { context: Context ->
            for (action in todo) {
                action(context)
            }
        }
    }
}

internal fun sendMessageAction(
    sendMessage: (OutboundMessage) -> Unit,
    text: String,
    postback: String,
): ButtonAction = { _ ->
    sendMessage(OutboundMessage(text, postback))
}

internal fun deepLinkAction(deepLink: String): ButtonAction = { context ->
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)))
}
