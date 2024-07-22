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

package com.nice.cxonechat.internal

import android.annotation.SuppressLint
import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import com.nice.cxonechat.ChatBuilder.OnChatBuiltResultCallback
import com.nice.cxonechat.ChatStateListener
import java.util.concurrent.CountDownLatch

internal class ChatBuilderThreading(
    private val origin: ChatBuilder,
    private val entrails: ChatEntrails,
) : ChatBuilder {

    private var listener: ChatStateListener? = null

    override fun setAuthorization(authorization: Authorization) = apply {
        origin.setAuthorization(authorization)
    }

    override fun setDevelopmentMode(enabled: Boolean) = apply {
        origin.setDevelopmentMode(enabled)
    }

    override fun setUserName(first: String, last: String) = apply {
        origin.setUserName(first, last)
    }

    override fun setChatStateListener(listener: ChatStateListener): ChatBuilder = apply {
        this.listener = listener
        origin.setChatStateListener(listener)
    }

    override fun setDeviceToken(token: String): ChatBuilder = apply {
        origin.setDeviceToken(token)
    }

    override fun setCustomerId(customerId: String): ChatBuilder = apply {
        origin.setCustomerId(customerId)
    }

    @Deprecated(
        "Please migrate to build method with OnChatBuildResultCallback",
        replaceWith = ReplaceWith(
            "build(resultCallback = OnChatBuiltResultCallback { callback.onChatBuilt(it.getOrThrow()) })",
            "com.nice.cxonechat.ChatBuilder.OnChatBuiltResultCallback"
        )
    )
    override fun build(callback: OnChatBuiltCallback): Cancellable =
        build(resultCallback = { chatResult -> callback.onChatBuilt(chatResult.getOrThrow()) })

    override fun build(
        resultCallback: OnChatBuiltResultCallback,
    ): Cancellable {
        val threading = entrails.threading
        return threading.background {
            val chat = buildSynchronous()
            threading.foreground {
                resultCallback.onChatBuiltResult(chat)
            }
        }
    }

    // ---

    @SuppressLint(
        "CheckResult" // Result is not used intentionally as cancellation is done via interrupt.
    )
    private fun buildSynchronous(): Result<Chat> {
        val latch = CountDownLatch(1)
        var chat: Result<Chat>? = null
        origin.build(resultCallback = {
            chat = it
            latch.countDown()
        })
        latch.await()
        return checkNotNull(chat)
    }
}
