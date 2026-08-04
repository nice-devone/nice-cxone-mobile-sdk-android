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

/**
 * Internal extension of the builder contract which cannot live on the public
 * [com.nice.cxonechat.ChatBuilder] interface (interface members are always public).
 *
 * Implemented by [ChatBuilderDefault] and forwarded by the builder decorators so callers within
 * the SDK can reach it with a safe cast regardless of decoration depth.
 */
internal interface ChatBuilderInternal {

    /**
     * When set to `true`, [com.nice.cxonechat.ChatBuilder.build] reuses the channel configuration
     * cached by a previous build (see [ChannelConfigurationCache]) instead of fetching it from the
     * backend. Used for per-session Chat instance rebuilds, where the configuration was already
     * fetched by the preceding [com.nice.cxonechat.ChatInstanceProvider.prepare] — the getChannel
     * REST call is costly on the backend and its result is fixed for the lifetime of a prepared
     * provider anyway.
     */
    fun setPreferCachedConfiguration(prefer: Boolean)
}
