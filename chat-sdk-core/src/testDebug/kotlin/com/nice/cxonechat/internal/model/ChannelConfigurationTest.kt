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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
import org.junit.Test
import kotlin.test.assertEquals

internal class ChannelConfigurationTest {

    @Test
    fun authenticationType_isSecuredCookieWhenSecuredCookieEnabled() {
        val config = createChannelConfiguration(
            isSecuredCookieEnabled = true,
            isAuthorizationEnabled = false,
        )

        val configuration = config.toConfiguration("channel-123")

        assertEquals(AuthenticationType.SecuredCookie, configuration.authenticationType)
    }

    @Test
    fun authenticationType_isThirdPartyOAuthWhenAuthorizationEnabled() {
        val config = createChannelConfiguration(
            isSecuredCookieEnabled = false,
            isAuthorizationEnabled = true,
        )

        val configuration = config.toConfiguration("channel-123")

        assertEquals(AuthenticationType.ThirdPartyOAuth, configuration.authenticationType)
    }

    @Test
    fun authenticationType_isAnonymousWhenBothDisabled() {
        val config = createChannelConfiguration(
            isSecuredCookieEnabled = false,
            isAuthorizationEnabled = false,
        )

        val configuration = config.toConfiguration("channel-123")

        assertEquals(AuthenticationType.Anonymous, configuration.authenticationType)
    }

    @Test
    fun authenticationType_prioritizesSecuredCookieWhenBothEnabled() {
        // This test validates the documented priority: SecuredCookie takes precedence over OAuth
        // when both isSecuredCookieEnabled and isAuthorizationEnabled are true.
        // See determineAuthenticationType() KDoc for important notes about ambiguous backend configuration.
        val config = createChannelConfiguration(
            isSecuredCookieEnabled = true,
            isAuthorizationEnabled = true,
        )

        val configuration = config.toConfiguration("channel-123")

        // IMPORTANT: SecuredCookie takes priority - this behavior should match backend intent
        assertEquals(AuthenticationType.SecuredCookie, configuration.authenticationType)
    }

    private fun createChannelConfiguration(
        isSecuredCookieEnabled: Boolean,
        isAuthorizationEnabled: Boolean,
    ): ChannelConfiguration {
        return ChannelConfiguration(
            settings = ChannelConfiguration.Settings(
                hasMultipleThreadsPerEndUser = false,
                isProactiveChatEnabled = false,
                liveChatAllowTranscript = false,
                securedSessions = false,
                fileRestrictions = ChannelConfiguration.FileRestrictions(
                    allowedFileSize = 10 * 1024 * 1024,
                    allowedFileTypes = emptyList(),
                    isAttachmentsEnabled = false,
                ),
                features = emptyMap(),
            ),
            isAuthorizationEnabled = isAuthorizationEnabled,
            isSecuredCookieEnabled = isSecuredCookieEnabled,
            preContactForm = null,
            isLiveChat = false,
            availability = ChannelConfiguration.Availability(status = Online),
        )
    }
}
