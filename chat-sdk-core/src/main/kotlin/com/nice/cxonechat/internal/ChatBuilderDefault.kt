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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.ChatThreadingImpl
import com.nice.cxonechat.TokenDelegateListener
import com.nice.cxonechat.exceptions.SdkVersionNotSupported
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ApiErrorModel
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.log.error
import com.nice.cxonechat.state.Connection
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class ChatBuilderDefault(
    private val entrails: ChatEntrails,
    private val factory: SocketFactory,
) : ChatBuilder, ChatBuilderInternal {

    private var isDevelopment: Boolean = false
    private var authorization: Authorization = Authorization.None
    private var firstName: String? = null
    private var lastName: String? = null
    private var chatStateListener: ChatStateListener? = null
    private var deviceToken: String? = null
    private var customerId: String? = null
    private var tokenDelegateListener: TokenDelegateListener? = null
    private var preferCachedChannelConfiguration: Boolean = false

    override fun setPreferCachedConfiguration(prefer: Boolean) {
        preferCachedChannelConfiguration = prefer
    }

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

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun setChatStateListener(listener: ChatStateListener): ChatBuilder = apply {
        chatStateListener = listener
    }

    override fun setDeviceToken(token: String): ChatBuilder = apply {
        deviceToken = token
    }

    override fun setCustomerId(customerId: String): ChatBuilder = apply {
        this.customerId = customerId
    }

    override fun setTokenDelegateListener(listener: TokenDelegateListener): ChatBuilder = apply {
        tokenDelegateListener = listener
    }

    override suspend fun build(): Chat {
        val state = BuilderState(
            isDevelopment = isDevelopment,
            authorization = authorization,
            firstName = firstName,
            lastName = lastName,
            chatStateListener = chatStateListener,
            deviceToken = deviceToken,
            customerId = customerId,
            tokenDelegateListener = tokenDelegateListener,
            preferCachedChannelConfiguration = preferCachedChannelConfiguration,
        )
        val chatParameters = prepareChatParameters(
            entrails = entrails,
            factory = factory,
            state = state
        )
        return createChatInstance(chatParameters, entrails, state)
    }
}

private data class BuilderState(
    val isDevelopment: Boolean,
    val authorization: Authorization,
    val firstName: String?,
    val lastName: String?,
    val chatStateListener: ChatStateListener?,
    val deviceToken: String?,
    val customerId: String?,
    val tokenDelegateListener: TokenDelegateListener?,
    val preferCachedChannelConfiguration: Boolean,
)

private data class ChatParameters(
    val connection: Connection,
    val socketFactory: SocketFactory,
    val body: ChannelConfiguration,
    val storeVisitorCallback: Callback<Void>,
)

private fun prepareChatParameters(
    entrails: ChatEntrails,
    factory: SocketFactory,
    state: BuilderState,
): ChatParameters {
    state.customerId?.let { id ->
        if (entrails.storage.customerId != id) entrails.storage.customerId = id
    }
    var connection = factory.getConfiguration(entrails.storage)
    val firstName = state.firstName
    val lastName = state.lastName
    if (firstName != null && lastName != null) {
        connection = connection.asCopyable().copy(
            firstName = firstName,
            lastName = lastName
        )
    }
    state.deviceToken?.let { entrails.storage.deviceToken = it }
    val cachedConfiguration = if (state.preferCachedChannelConfiguration) {
        ChannelConfigurationCache.get(connection.brandId, connection.channelId)
    } else {
        null
    }
    val body = cachedConfiguration ?: fetchChannelConfiguration(entrails, connection).also {
        ChannelConfigurationCache.put(connection.brandId, connection.channelId, it)
    }
    val storeVisitorCallback = if (state.isDevelopment) StoreVisitorCallback(entrails.logger) else IgnoredCallback
    return ChatParameters(connection, factory, body, storeVisitorCallback)
}

private fun createChatInstance(
    chatParameters: ChatParameters,
    entrails: ChatEntrails,
    state: BuilderState,
): ChatWithParameters {
    val storeVisitorCallback: Callback<Void> = chatParameters.storeVisitorCallback
    val configurationInternal = chatParameters.body.toConfiguration(chatParameters.connection.channelId)
    var chat: ChatWithParameters
    chat = ChatImpl(
        connection = chatParameters.connection,
        entrails = entrails,
        dependencies = ChatImplDependencies(
            socketFactory = chatParameters.socketFactory,
            callback = storeVisitorCallback,
            authorization = state.authorization,
        ),
        configuration = configurationInternal,
        rawChatStateListener = state.chatStateListener,
        tokenDelegateListener = state.tokenDelegateListener,
    )
    chat = ChatS3Events(chat)
    chat = ChatStoreVisitor(chat, storeVisitorCallback)
    chat = ChatWelcomeMessageUpdate(chat)
    chat = ChatMemoizeThreadsHandler(chat)
    chat = when (chat.chatMode) {
        SingleThread -> ChatSingleThread(chat)
        MultiThread -> ChatMultiThread(chat)
        LiveChat -> ChatLiveChat(chat)
    }
    chat = ChatServerErrorReporting(chat)
    chat = ChatReconnectWebsocket(chat)
    chat = ChatMemoizeThreadsHandler(chat)
    chat = ChatThreadingImpl(chat)
    if (state.isDevelopment) chat = ChatLogging(chat)
    return chat
}

private fun fetchChannelConfiguration(entrails: ChatEntrails, connection: Connection): ChannelConfiguration {
    val response = entrails.service.getChannel(connection.brandId.toString(), connection.channelId).execute()
    if (!response.isSuccessful) {
        val errorBody = response.errorBody()?.string()
        val apiError = errorBody?.let {
            Default.serializer.decodeFromString<ApiErrorModel?>(it)
        }
        val errorCode = apiError?.error?.errorCode
        if (errorCode == "SdkVersionNotSupported") {
            throw SdkVersionNotSupported(
                apiError.error.errorMessage ?: "Your version of SDK is not supported anymore, please do upgrade."
            )
        } else {
            entrails.logger.error("Failed to fetch channel configuration: ${response.code()} - ${response.message()} - $errorBody")
            error("Response from the server was not successful")
        }
    }
    return checkNotNull(response.body()) { "Response body was null" }
}

private object IgnoredCallback : Callback<Void> {
    override fun onResponse(call: Call<Void>, response: Response<Void>) = Unit
    override fun onFailure(p0: Call<Void>, p1: Throwable) = Unit
}
