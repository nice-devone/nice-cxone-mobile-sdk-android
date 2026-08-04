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

import com.nice.cxonechat.internal.model.ChannelConfiguration

/**
 * In-memory cache of the last fetched [ChannelConfiguration], keyed by brand and channel.
 *
 * Every successful build stores the fetched configuration here; it is READ only when a builder was
 * explicitly asked to prefer it (see [ChatBuilderInternal.setPreferCachedConfiguration]) —
 * per-session Chat rebuilds reuse the configuration fetched at prepare() time instead of repeating
 * the costly getChannel REST call. Freshness semantics are unchanged: a reused Chat instance never
 * re-fetched its configuration either ("The saved configuration will never change at run-time").
 *
 * The cache is process-scoped and cleared on [com.nice.cxonechat.ChatInstanceProvider.signOut].
 */
internal object ChannelConfigurationCache {

    private data class Entry(
        val brandId: Int,
        val channelId: String,
        val configuration: ChannelConfiguration,
    )

    @Volatile
    private var entry: Entry? = null

    fun get(brandId: Int, channelId: String): ChannelConfiguration? = entry
        ?.takeIf { it.brandId == brandId && it.channelId == channelId }
        ?.configuration

    fun put(brandId: Int, channelId: String, configuration: ChannelConfiguration) {
        entry = Entry(brandId, channelId, configuration)
    }

    fun clear() {
        entry = null
    }
}
