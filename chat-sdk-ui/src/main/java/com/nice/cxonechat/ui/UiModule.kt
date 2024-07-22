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

package com.nice.cxonechat.ui

import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.ui.data.PinpointPushMessageParser
import com.nice.cxonechat.ui.domain.PushMessageParser
import com.nice.cxonechat.utilities.TaggingSocketFactory
import okhttp3.OkHttpClient
import org.koin.core.KoinApplication
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.module

@Module
@ComponentScan("com.nice.cxonechat.ui")
@Suppress("UndocumentedPublicClass")
class UiModule internal constructor() {
    @Factory
    internal fun produceChatInstanceProvider() = ChatInstanceProvider.get()

    @Factory
    internal fun produceChat() = requireNotNull(produceChatInstanceProvider().chat)

    @Singleton
    internal fun produceOkHttpClient() = OkHttpClient.Builder()
        .socketFactory(TaggingSocketFactory)
        .build()

    companion object {
        internal const val loggerName = "com.nice.cxonechat.ui.logger"

        /**
         * Initialize the Ui Module.
         *
         * Invoked as:
         *
         * ```
         *    startKoin {
         *      UiModule.setup()
         *    }
         * ```
         *
         * @note Must be called before [ChatActivity] is created.
         *
         * @receiver KoinApplication instance to configure.
         * @param logger Logger to use if logging is desired.
         */
        fun KoinApplication.chatUiModule(logger: Logger = LoggerNoop) {
            modules(
                listOf(
                    UiModule().module,
                    module {
                        factoryOf(::PinpointPushMessageParser).bind(PushMessageParser::class)
                        factory(named(loggerName)) { logger }
                    }
                )
            )
        }
    }
}
