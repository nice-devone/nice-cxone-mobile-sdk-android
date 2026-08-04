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

package com.nice.cxonechat

import android.content.Context
import com.nice.cxonechat.core.BuildConfig
import com.nice.cxonechat.internal.ChatBuilderDefault
import com.nice.cxonechat.internal.ChatBuilderLogging
import com.nice.cxonechat.internal.ChatBuilderThreading
import com.nice.cxonechat.internal.ChatEntrails
import com.nice.cxonechat.internal.ChatEntrailsAndroid
import com.nice.cxonechat.internal.Threading
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.SocketFactoryDefault
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.ProxyLogger
import com.nice.cxonechat.logger.RemoteLogger
import com.nice.cxonechat.storage.CookieJarProvider
import com.nice.cxonechat.storage.PreferencesValueStorage
import com.nice.cxonechat.util.UserAgent.addUserAgentInterceptor
import com.nice.cxonechat.utilities.TaggingSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import io.github.ackeecz.useragent.UserAgent as AckeeUserAgent

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
     */
    @Deprecated("Use Chat.stateFlow to observe state changes instead.")
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
     * Sets an optional [TokenDelegateListener] for the implicit OAuth flow.
     *
     * When set, the SDK delegates all token acquisition to the integrator. The SDK will call
     * [TokenDelegateListener.onNewTokenRequested] on a background thread in the following cases:
     * - **Initial connection** ([TokenRequestReason.UNSPECIFIED]): called during the first
     *   [Chat.connect] to obtain the JWT needed to establish the socket connection.
     * - **Proactive expiry** ([TokenRequestReason.TOKEN_EXPIRED]): called when the stored token
     *   is about to expire during an active session.
     * - **Backend rejection** ([TokenRequestReason.TOKEN_INVALID]): called when the backend
     *   rejects the stored JWT via HTTP 401.
     *
     * The call blocks until the integrator returns a token or throws.
     * Returning a JWT string resumes the pending operation; throwing causes the SDK to report a
     * [com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException].
     *
     * **Required for the implicit OAuth flow.** Without this listener the SDK cannot obtain or
     * refresh a token and will report
     * [com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError].
     *
     * This listener is only applicable to the **implicit OAuth flow**. It has no effect in
     * the explicit flow (auth code + code verifier via [setAuthorization]).
     *
     * Defaults to `null` (no token delegation — SDK manages tokens internally).
     */
    fun setTokenDelegateListener(listener: TokenDelegateListener): ChatBuilder

    /**
     * Builds an instance of [Chat].
     * Any standard issue which may happen during the build process is thrown as [IllegalStateException].
     * All failures are logged if [setDevelopmentMode] is set.
     *
     * If the instance is not retrieved within a reasonable amount of time, the
     * device is not connected to the internet, or the chat provider experiences
     * outage or your instance is misconfigured. In all of these cases, consult
     * a representative.
     *
     * This is a suspend function \u2014 call it from a coroutine context.
     *
     * @return A [Chat] instance ready for use.
     */
    suspend fun build(): Chat

    @Public
    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {

        /**
         * Returns an instance of [ChatBuilder] with Android specific parameters.
         *
         * This is a suspend function that performs I/O for HTTP client setup.
         * Java callers must use `ChatBuilderJavaInterop.getDefaultBlocking(...)` or
         * `ChatBuilderJavaInterop.getDefaultAsync(...)` from the `chat-sdk-core-java` module.
         *
         * @param context The [Context] used for persistent storage of values by the SDK.
         * @param config [SocketFactoryConfiguration] connection configuration of the chat.
         * @param logger [Logger] which will be used by the builder and the SDK, default is no-op implementation.
         *
         * @see build
         * */
        @JvmSynthetic
        suspend operator fun invoke(
            context: Context,
            config: SocketFactoryConfiguration,
            logger: Logger = LoggerNoop,
        ): ChatBuilder = getDefault(context, config, logger)

        /**
         * Returns an instance of [ChatBuilder] with Android specific parameters.
         *
         * This is a suspend function that performs I/O for HTTP client setup.
         * Java callers must use `ChatBuilderJavaInterop.getDefaultBlocking(...)` or
         * `ChatBuilderJavaInterop.getDefaultAsync(...)` from the `chat-sdk-core-java` module.
         *
         * @param context The [Context] used for persistent storage of values by the SDK.
         * @param config [SocketFactoryConfiguration] connection configuration of the chat.
         * @param logger [Logger] which will be used by the builder and the SDK, default is no-op implementation.
         *
         * @see build
         * */
        @JvmSynthetic
        suspend fun getDefault(
            context: Context,
            config: SocketFactoryConfiguration,
            logger: Logger = LoggerNoop,
        ): ChatBuilder {
            val cookieJar = CookieJarProvider.getInstance(context, logger = logger)
            val sharedClient =
                withContext(Dispatchers.IO) {
                    OkHttpClient()
                        .newBuilder()
                        .cookieJar(cookieJar)
                        .addUserAgentInterceptor(AckeeUserAgent(context.applicationContext))
                        .addInterceptor { chain ->
                            chain.proceed(
                                chain.request()
                                    .newBuilder()
                                    .addHeader("x-sdk-platform", "android")
                                    .addHeader("x-sdk-version", BuildConfig.VERSION_NAME)
                                    .build()
                            )
                        }
                        .socketFactory(TaggingSocketFactory)
                        .build()
                }

            val factory = SocketFactoryDefault(config, sharedClient, logger)
            val updateLogger = ProxyLogger(
                RemoteLogger(BuildConfig.VERSION_NAME, sharedClient, logger),
                logger
            )
            val threading = Threading(updateLogger)
            val storage = PreferencesValueStorage.create(context.applicationContext, threading.storageWriteScope, updateLogger)
            val entrails = ChatEntrailsAndroid(
                factory = factory,
                config = config,
                sharedClient = sharedClient,
                logger = updateLogger,
                cookieJar = cookieJar,
                storage = storage,
                threading = threading,
            )
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
