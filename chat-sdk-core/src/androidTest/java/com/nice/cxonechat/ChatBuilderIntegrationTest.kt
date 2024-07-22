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

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.nice.cxonechat.ChatBuilder.OnChatBuiltResultCallback
import com.nice.cxonechat.internal.model.EnvironmentInternal
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

@SmallTest
internal class ChatBuilderIntegrationTest {

    @Test
    fun connectsToServer() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val environment = EnvironmentInternal(
            name = "",
            location = "",
            baseUrl = "https://channels-eu1-qa.brandembassy.com/",
            socketUrl = "wss://chat-gateway-eu1-qa.brandembassy.com",
            originHeader = "https://livechat-eu1-qa.brandembassy.com",
            chatUrl = "https://channels-eu1-qa.brandembassy.com/chat/"
        )
        val config = SocketFactoryConfiguration(environment, 6450, "chat_f62c9eaf-f030-4d0d-aa87-6e8a5aed3c55")
        val latch = CountDownLatch(1)
        val cancellable = ChatBuilder(context, config)
            .setDevelopmentMode(true)
            .setUserName("john", "doe")
            .build(resultCallback = {
                it.getOrThrow().close()
                latch.countDown()
            })
        assertTrue(latch.await(10, SECONDS))
        cancellable.cancel()
    }

}
