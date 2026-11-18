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

package com.nice.cxonechat.ui.util

import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.FileRestrictions
import org.junit.rules.ExternalResource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Serves [fakeConfiguration] through Koin, which the chat UI resolves before it lays anything out.
 *
 * Koin's registry is process wide, so it has to be stopped again: a class that leaves it running fails the
 * next one with "A Koin Application has already been started".
 */
internal class ChatConfigurationRule(
    private val configuration: Configuration = fakeConfiguration(),
) : ExternalResource() {

    override fun before() {
        startKoin {
            modules(
                module {
                    single<Configuration?> { configuration }
                }
            )
        }
    }

    override fun after() {
        stopKoin()
    }
}

/**
 * The configuration a rendering test needs answered, all features off.
 *
 * [Configuration] is an interface the SDK keeps extending, so a new abstract member has to be answered here
 * for the tests using [ChatConfigurationRule] — the screenshot tests and `src/androidTest` still build their
 * own, sharing no code with this one.
 */
internal fun fakeConfiguration(isAttachmentsEnabled: Boolean = false): Configuration = object : Configuration {
    override val hasMultipleThreadsPerEndUser: Boolean = true
    override val isProactiveChatEnabled: Boolean = false
    override val isAuthorizationEnabled: Boolean = false
    override val liveChatAllowTranscript: Boolean = false
    override val isSecuredCookieEnabled: Boolean = false

    // Deprecated upstream, but still abstract, so a fake has to answer it.
    @Suppress("OVERRIDE_DEPRECATION")
    override val securedSessions: Boolean = false
    override val fileRestrictions: FileRestrictions = object : FileRestrictions {
        override val allowedFileSize: Int = 40
        override val allowedFileTypes: List<FileRestrictions.AllowedFileType> = emptyList()
        override val isAttachmentsEnabled: Boolean = isAttachmentsEnabled
    }
    override val isLiveChat: Boolean = false
    override val isOnline: Boolean = false
    override fun hasFeature(feature: String): Boolean = false
}
