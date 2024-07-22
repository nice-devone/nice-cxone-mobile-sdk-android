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

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import com.nice.cxonechat.ChatBuilder.OnChatBuiltResultCallback
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.ChatThreadingImpl
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.state.Connection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class ChatBuilderDefault(
    private val entrails: ChatEntrails,
    private val factory: SocketFactory,
) : ChatBuilder {

    private var isDevelopment: Boolean = false
    private var authorization: Authorization = Authorization.None
    private var firstName: String? = null
    private var lastName: String? = null
    private var chatStateListener: ChatStateListener? = null
    private var deviceToken: String? = null
    private var customerId: String? = null

    override fun setAuthorization(authorization: Authorization) = apply {
        this.authorization = authorization
    }

    override fun setDevelopmentMode(enabled: Boolean) = apply {
        this.isDevelopment = enabled
    }

    override fun setUserName(first: String, last: String) = apply {
        this.firstName = first
        this.lastName = last
    }

    override fun setChatStateListener(listener: ChatStateListener): ChatBuilder = apply {
        chatStateListener = listener
    }

    override fun setDeviceToken(token: String): ChatBuilder = apply {
        deviceToken = token
    }

    override fun setCustomerId(customerId: String): ChatBuilder = apply {
        this.customerId = customerId
    }

    @Deprecated(
        message = "Please migrate to build method with OnChatBuildResultCallback",
        replaceWith = ReplaceWith(
            expression = "build(resultCallback = OnChatBuiltResultCallback { callback.onChatBuilt(it.getOrThrow()) })",
            imports = ["com.nice.cxonechat.ChatBuilder.OnChatBuiltResultCallback"]
        )
    )
    override fun build(callback: OnChatBuiltCallback): Cancellable =
        build(resultCallback = { chatResult -> callback.onChatBuilt(chatResult.getOrThrow()) })

    override fun build(resultCallback: OnChatBuiltResultCallback): Cancellable {
        resultCallback.onChatBuiltResult(
            runCatching {
                val chatParameters = prepareChatParameters()
                createChatInstance(chatParameters)
            }
        )
        return Cancellable.noop
    }

    private fun prepareChatParameters(): ChatParameters {
        customerId?.let { id ->
            val currentId = entrails.storage.customerId
            if (currentId != id) {
                if (currentId != null) entrails.storage.clearStorage()
                entrails.storage.customerId = id
            }
        }
        var connection = factory.getConfiguration(entrails.storage)
        val firstName = firstName
        val lastName = lastName
        if (firstName != null && lastName != null) {
            connection = connection.asCopyable().copy(
                firstName = firstName,
                lastName = lastName,
            )
        }
        deviceToken?.let { entrails.storage.deviceToken = it }
        val response = entrails.service.getChannel(connection.brandId.toString(), connection.channelId).execute()
        check(response.isSuccessful) { "Response from the server was not successful" }
        val body = checkNotNull(response.body()) { "Response body was null" }
        val storeVisitorCallback = if (isDevelopment) StoreVisitorCallback(entrails.logger) else IgnoredCallback
        return ChatParameters(connection, factory, body, storeVisitorCallback)
    }

    private fun createChatInstance(
        chatParameters: ChatParameters,
    ): ChatWithParameters {
        val storeVisitorCallback: Callback<Void> = chatParameters.storeVisitorCallback
        var chat: ChatWithParameters
        chat = ChatImpl(
            connection = chatParameters.connection,
            entrails = entrails,
            socketFactory = chatParameters.socketFactory,
            configuration = chatParameters.body.toConfiguration(chatParameters.connection.channelId),
            callback = storeVisitorCallback,
            chatStateListener = chatStateListener
        )
        chat = ChatS3Events(chat)
        chat = ChatAuthorization(chat, authorization)
        chat = ChatStoreVisitor(chat, storeVisitorCallback)
        chat = ChatWelcomeMessageUpdate(chat)
        chat = ChatServerErrorReporting(chat)
        chat = ChatMemoizeThreadsHandler(chat)
        chat = when (chat.chatMode) {
            SingleThread -> ChatSingleThread(chat)
            MultiThread -> ChatMultiThread(chat)
            LiveChat -> ChatLiveChat(chat)
        }
        chat = ChatMemoizeThreadsHandler(chat)
        chat = ChatThreadingImpl(chat)
        if (isDevelopment) chat = ChatLogging(chat)
        return chat
    }
}

private data class ChatParameters(
    val connection: Connection,
    val socketFactory: SocketFactory,
    val body: ChannelConfiguration,
    val storeVisitorCallback: Callback<Void>,
)

private object IgnoredCallback : Callback<Void> {
    override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
    override fun onFailure(p0: Call<Void>, p1: Throwable) = Unit
}
