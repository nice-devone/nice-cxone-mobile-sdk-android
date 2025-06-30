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

package com.nice.cxonechat.ui.screen

import kotlinx.serialization.Serializable

/**
 * Represents the possible screens in the chat UI navigation.
 *
 * This sealed class defines all the possible screen states that can be represented
 * in the chat UI module's navigation system. Each screen is serializable to support
 * state persistence across configuration changes or process death.
 *
 * The class is marked as internal to ensure it's only used within the UI module
 * and not exposed to SDK integrators.
 */
@Serializable
internal sealed class Screen {

    /** Screen shown when the chat is in Live Chat mode, but configuration indicates the channel is offline. */
    @Serializable
    data object Offline : Screen()

    /** Screen shown when the chat is in Messaging mode and the channel is in multi-thread mode and user should see
     * a selection of existing conversations.
     */
    @Serializable
    data object ThreadList : Screen()

    /** Screen shown when displaying one concreate conversation, it is used in all modes. */
    @Serializable
    data object ThreadScreen : Screen()
}
