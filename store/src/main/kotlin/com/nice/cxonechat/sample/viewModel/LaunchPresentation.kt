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

package com.nice.cxonechat.sample.viewModel

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * What the automated launch path is showing.
 *
 * Kept apart from the handler which drives the chat: these answer a different question, and they are
 * read by different callers -- the activity asks whether to build the storefront, and
 * [UiState.OpeningChat] asks whether the chat still needs opening.
 *
 * @param pendingLaunchConfiguration Request the activity deposited before the ViewModel existed. The
 * storefront starts hidden whenever one is waiting, so it is suppressed from the very first
 * composition rather than after the ViewModel's asynchronous initialisation has caught up.
 */
class LaunchPresentation(pendingLaunchConfiguration: PendingLaunchConfiguration) {

    private val storefrontVisible = MutableStateFlow(!pendingLaunchConfiguration.isPending)
    private val openChatPending = AtomicBoolean(false)

    /**
     * True when the storefront itself should be built.
     *
     * False while a launch intent is driving the app towards the chat: the store is only worth
     * loading once somebody is actually going to look at it, which is when they come back from the
     * chat, or when the setup ends without opening it.
     */
    @Stable
    val isStorefrontVisible = storefrontVisible.asStateFlow()

    /** True while the chat has been asked for but not yet opened. */
    val isOpenChatPending: Boolean
        get() = openChatPending.get()

    /** Give up on building the storefront while the launch path drives towards the chat. */
    fun hideStorefront() {
        storefrontVisible.value = false
    }

    /** Build the storefront again, because somebody is about to look at it. */
    fun showStorefront() {
        storefrontVisible.value = true
    }

    /** Ask for the chat to be opened, keeping the storefront unbuilt until it is left again. */
    fun requestOpenChat() {
        openChatPending.set(true)
        storefrontVisible.value = false
    }

    /**
     * Take the pending request to open the chat, if there is one.
     *
     * One-shot, so that recreating the activity while [UiState.OpeningChat] is current -- which a
     * rotation while the chat is in front does -- doesn't open a second chat.
     */
    fun consumeOpenChatRequest(): Boolean = openChatPending.getAndSet(false)
}
