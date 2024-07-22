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

import android.content.Context
import android.os.Looper
import androidx.core.os.HandlerCompat
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.storage.PreferencesValueStorage
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.OkHttpClient
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

internal class ChatEntrailsAndroid(
    context: Context,
    factory: SocketFactory,
    config: SocketFactoryConfiguration,
    override val sharedClient: OkHttpClient,
    override val logger: Logger,
) : ChatEntrails {

    override val storage: ValueStorage by lazy { PreferencesValueStorage(context) }
    override val service: RemoteService by lazy {
        RemoteServiceBuilder()
        .setSharedOkHttpClient(sharedClient)
        .setConnection(factory.getConfiguration(storage))
        .build()
    }
    override val threading: Threading = Threading(AndroidExecutor())
    override val environment: Environment = config.environment

    private class AndroidExecutor : AbstractExecutorService() {

        private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

        override fun execute(command: Runnable?) {
            handler.post(command ?: return)
        }

        override fun shutdown() = Unit
        override fun shutdownNow() = emptyList<Runnable>()
        override fun isShutdown() = false
        override fun isTerminated() = false
        override fun awaitTermination(timeout: Long, unit: TimeUnit?) = false
    }
}
