/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui

import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.Public
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.NoExtraCustomFields
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.PinpointPushMessageParser
import com.nice.cxonechat.ui.domain.PushMessageParser
import com.nice.cxonechat.utilities.TaggingSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.core.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.KoinConfiguration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.module
import javax.inject.Singleton

/**
 * Koin module for the UI layer of the application.
 *
 * This module provides dependencies required for the UI layer, such as
 * notification management, logging, and chat-related services.
 */
@Module
@ComponentScan("com.nice.cxonechat.ui")
class UiModule internal constructor() {

    /**
     * Provides an instance of [ChatInstanceProvider].
     *
     * @return The [ChatInstanceProvider] instance.
     */
    @Factory
    internal fun produceChatInstanceProvider() = ChatInstanceProvider.get()

    /**
     * Provides the chat instance from the [ChatInstanceProvider].
     *
     * @return The chat instance.
     * @throws IllegalStateException if the chat instance is null.
     */
    @Factory
    internal fun produceChat() = requireNotNull(produceChatInstanceProvider().chat)

    /**
     * Provides a singleton instance of [OkHttpClient] configured with a custom socket factory.
     *
     * @return The [OkHttpClient] instance.
     */
    @Singleton
    internal fun produceOkHttpClient() = runBlocking(Dispatchers.IO) {
        OkHttpClient.Builder()
            .socketFactory(TaggingSocketFactory)
            .build()
    }

    @Suppress("UndocumentedPublicClass")
    companion object {
        internal const val LOGGER_NAME = "com.nice.cxonechat.ui.logger"

        /**
         * Configures the UI module for Koin dependency injection.
         *
         * This method sets up the required dependencies for the UI layer, including
         * notification management, logging, and chat-related services.
         *
         * Example usage:
         * ```
         *    startKoin {
         *      UiModule.chatUiModule()
         *    }
         * ```
         *
         * @note Must be called before [com.nice.cxonechat.ui.screen.ChatActivity] is created.
         *
         * @receiver The [KoinApplication] instance to configure.
         * @param logger The [Logger] to use for logging. Defaults to [LoggerNoop].
         * @param customerFieldsProvider [UiCustomFieldsProvider] for extra customer field definitions, default is [NoExtraCustomFields].
         * @param contactFieldsProvider [UiCustomFieldsProvider] for extra contact field definitions, default is [NoExtraCustomFields].
         */
        @Public
        fun chatUiModule(
            logger: Logger = LoggerNoop,
            customerFieldsProvider: UiCustomFieldsProvider = NoExtraCustomFields,
            contactFieldsProvider: UiCustomFieldsProvider = NoExtraCustomFields,
        ) = KoinConfiguration {
            modules(
                module {
                    // Provides a factory for parsing push messages from the Amazon Pinpoint.
                    factoryOf(::PinpointPushMessageParser).bind(PushMessageParser::class)
                    // Provides a named logger instance.
                    factory(named(LOGGER_NAME)) { logger }
                    single(named(CustomFieldProviderType.Customer)) { customerFieldsProvider }
                    single(named(CustomFieldProviderType.Contact)) { contactFieldsProvider }
                    factory { runCatching { ChatInstanceProvider.get().chat?.configuration }.getOrNull() }
                },
                UiModule().module,
            )
        }
    }
}
