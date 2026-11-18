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

package com.nice.cxonechat.ui.composable.screenshot

import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FileRestrictions
import org.junit.rules.ExternalResource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Supplies the [Configuration] a conversation resolves from Koin while it composes, and tears the graph down
 * after the test.
 *
 * One answer does reach a pixel: attachments disabled leaves the add-attachment button out of the input bar, which
 * every capture includes. Every value is whichever keeps the conversation on its plainest path, and is the value
 * the other conversation captures already supply.
 */
internal class ConversationKoinRule : ExternalResource() {

    override fun before() {
        startKoin {
            modules(
                module {
                    single<Configuration?> { ScreenshotConfiguration }
                }
            )
        }
    }

    override fun after() {
        stopKoin()
    }

    private object ScreenshotConfiguration : Configuration {
        override val hasMultipleThreadsPerEndUser = true
        override val isProactiveChatEnabled = false
        override val isAuthorizationEnabled = false
        override val liveChatAllowTranscript = false
        override val isSecuredCookieEnabled = false
        override val securedSessions = false
        override val isLiveChat = false
        override val isOnline = false

        override val fileRestrictions = object : FileRestrictions {
            override val allowedFileSize = 40
            override val allowedFileTypes: List<FileRestrictions.AllowedFileType> = emptyList()
            override val isAttachmentsEnabled = false
        }

        override fun hasFeature(feature: String) = false
    }
}
