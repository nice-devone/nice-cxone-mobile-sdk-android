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

import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.api.AuthServiceBuilder
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.storage.PersistentCookieJar
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.OkHttpClient

@Suppress("LongParameterList")
internal class ChatEntrailsAndroid(
    factory: SocketFactory,
    config: SocketFactoryConfiguration,
    override val sharedClient: OkHttpClient,
    override val logger: Logger,
    override val cookieJar: PersistentCookieJar,
    override val storage: ValueStorage,
    override val threading: Threading,
) : ChatEntrails {
    override val service: RemoteService by lazy {
        RemoteServiceBuilder()
            .setSharedOkHttpClient(sharedClient)
            .setConnection(factory.getConfiguration(storage))
            .build()
    }

    override val authService: AuthService by lazy {
        AuthServiceBuilder()
            .setSharedOkHttpClient(sharedClient)
            .setConnection(factory.getConfiguration(storage))
            .build()
    }

    override val environment: Environment = config.environment
}
