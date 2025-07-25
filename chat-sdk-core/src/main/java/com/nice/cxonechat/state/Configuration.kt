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

package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/**
 * The various options for how a channel is configured.
 */
@Suppress("ComplexInterface")
@Public
interface Configuration {

    /** Whether the channel supports multiple threads for the same user. */
    val hasMultipleThreadsPerEndUser: Boolean

    /** Whether the channel supports proactive chat features. */
    val isProactiveChatEnabled: Boolean

    /** Whether OAuth authorization is enabled for the channel. */
    val isAuthorizationEnabled: Boolean

    /** File attachment restrictions. */
    val fileRestrictions: FileRestrictions

    /** True iff this is a live chat. */
    val isLiveChat: Boolean

    /** True iff services are online. */
    val isOnline: Boolean

    /**
     * Check if a given feature is supported by string.
     *
     * **Note:** This exists only to test for features unknown at release time.
     * All other features should be checked via [hasFeature(Feature)]
     *
     * @param feature Feature to test.
     */
    fun hasFeature(feature: String): Boolean

    /**
     * Check if a given feature is supported by Feature name.
     *
     * @param feature Feature to test.
     */
    fun hasFeature(feature: Feature) = hasFeature(feature.key)

    /**
     * List of features known at release time.
     */
    @Public
    enum class Feature(internal val key: String) {
        /** If true indicates that the live chat logo should not be displayed. */
        LiveChatLogoHidden("liveChatLogoHidden"),

        /** If true indicates that chat is available proactively. */
        ProactiveChatEnabled("isProactiveChatEnabled"),

        /**
         * If true indicates that RecoverLiveChat will not fail if no live chat thread
         * is currently available, but rather, a new thread will be created if none
         * currently exists.
         */
        RecoverLiveChatDoesNotFail("isRecoverLivechatDoesNotFailEnabled")
    }
}
