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

package com.nice.cxonechat

import android.content.Context
import androidx.annotation.CheckResult
import com.nice.cxonechat.internal.ChatBuilderDefault
import com.nice.cxonechat.internal.ChatBuilderLogging
import com.nice.cxonechat.internal.ChatBuilderThreading
import com.nice.cxonechat.internal.ChatEntrails
import com.nice.cxonechat.internal.ChatEntrailsAndroid
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.SocketFactoryDefault
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.utilities.TaggingSocketFactory
import okhttp3.OkHttpClient

/**
 * Definition of builder used to create [Chat] instance.
 *
 * All options in the builder are now optional, but it is recommended to set either
 * authorization (to the non-default value) or username.
 */
@Public
interface ChatBuilder {

    /**
     * Sets authorization for newly created instance of the chat. It's used in the
     * case where Authorization is enabled in the backend configuration.
     *
     * Defaults to [Authorization.None].
     * */
    fun setAuthorization(authorization: Authorization): ChatBuilder

    /**
     * Sets a development mode. This can have various effects throughout the SDK
     * such as verbose logging.
     *
     * Defaults to `false`.
     * */
    fun setDevelopmentMode(enabled: Boolean): ChatBuilder

    /**
     * Sets a default username.
     * If the username changes invoke this method again and build the new chat to apply
     * the changes.
     * Usually, this should be done with app's lifecycle events automatically.
     * Name is updated with every eligible event, likely will be updated during
     * the authorization step when running [build].
     *
     * Defaults to empty values.
     * */
    fun setUserName(first: String, last: String): ChatBuilder

    /**
     * Sets optional [ChatStateListener] which will be notified about changes to
     * availability of chat functionality.
     * It is highly recommended to supply this listener.
     */
    fun setChatStateListener(listener: ChatStateListener): ChatBuilder

    /**
     * Sets optional device token of for purpose of receiving push messages.
     */
    fun setDeviceToken(token: String): ChatBuilder

    /**
     * Sets optional customerId.
     * If the customerId is specified, and it differs from the previous one, the chat will
     * reset any persistent data.
     * If no customerId is specified, the chat will use the persisted one, if none is available it will be acquired
     * from the server once the chat user is authorized.
     */
    fun setCustomerId(customerId: String): ChatBuilder

    /**
     * Build an instance of chat asynchronously.
     * Previously this method guaranteed an instance to be returned via [callback], this is no
     * longer the case. If there is an communication issue with the server, this method will throw a runtime exception.
     */
    @CheckResult
    @Deprecated(
        message = "Please migrate to build method with OnChatBuildResultCallback",
        replaceWith = ReplaceWith(
            expression = "build(resultCallback = OnChatBuiltResultCallback { callback.onChatBuilt(it.getOrThrow()) })",
            imports = ["com.nice.cxonechat.ChatBuilder.OnChatBuiltResultCallback"]
        )
    )
    fun build(callback: OnChatBuiltCallback): Cancellable

    /**
     * Builds an instance of chat asynchronously.
     * Any standard issue which may happen during will be reported as [IllegalStateException] in [Result.onFailure].
     * All failures are logged if [setDevelopmentMode] is set.
     *
     * If the instance is not retrieved within a reasonable amount of time, the
     * device is not connected to the internet, or the chat provider experiences
     * outage or your instance is misconfigured. In all of these cases, consult
     * a representative.
     *
     * Can be called from any thread, but will change to non-main thread immediately.
     *
     * @see OnChatBuiltCallback.onChatBuilt
     *
     * @return A [Cancellable] which allows to cancel the asynchronous operation.
     */
    @CheckResult
    fun build(resultCallback: OnChatBuiltResultCallback): Cancellable

    /**
     * Callback allowing to listen to chat instance provisioning.
     * @see build
     * */
    @Public
    fun interface OnChatBuiltCallback {
        /**
         * Notifies the consumer that a chat instance is ready. It's always called
         * on the main thread.
         * */
        fun onChatBuilt(chat: Chat)
    }

    /**
     * Callback allowing to listen to chat instance provisioning.
     */
    @Public
    fun interface OnChatBuiltResultCallback {
        /**
         * Notifies the consumer if the chat instance preparation has succeeded and provides the instance in the
         * case of the success.
         * It's always called on the main thread.
         */
        fun onChatBuiltResult(chat: Result<Chat>)
    }

    @Public
    companion object {

        /**
         * Returns an instance of [ChatBuilder] with Android specific parameters.
         *
         * @param context The [Context] used for persistent storage of values by the SDK.
         * @param config [SocketFactoryConfiguration] connection configuration of the chat.
         * @param logger [Logger] which will be used by the builder and the SDK, default is no-op implementation.
         *
         * @see build
         * @see OnChatBuiltCallback
         * @see OnChatBuiltCallback.onChatBuilt
         * */
        @JvmName("getDefault")
        @JvmOverloads
        @JvmStatic
        operator fun invoke(
            context: Context,
            config: SocketFactoryConfiguration,
            logger: Logger = LoggerNoop,
        ): ChatBuilder {
            val sharedClient = OkHttpClient()
                .newBuilder()
                .socketFactory(TaggingSocketFactory)
                .build()
            val factory = SocketFactoryDefault(config, sharedClient)
            val entrails = ChatEntrailsAndroid(context.applicationContext, factory, config, sharedClient, logger)
            return invoke(
                entrails = entrails,
                factory = factory
            )
        }

        @JvmSynthetic
        internal operator fun invoke(
            entrails: ChatEntrails,
            factory: SocketFactory,
        ): ChatBuilder {
            var builder: ChatBuilder
            builder = ChatBuilderDefault(entrails, factory)
            builder = ChatBuilderLogging(builder, entrails)
            builder = ChatBuilderThreading(builder, entrails)
            return builder
        }
    }
}
