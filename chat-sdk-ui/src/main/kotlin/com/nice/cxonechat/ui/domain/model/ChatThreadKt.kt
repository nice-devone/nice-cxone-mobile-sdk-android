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

package com.nice.cxonechat.ui.domain.model

import com.nice.cxonechat.thread.ChatThread

/**
 * Extension function for `ChatThread` that retrieves the thread name or agent's name.
 *
 * Priority order:
 * 1. Thread name (if multi-thread mode is enabled and name is not blank)
 * 2. Agent's nickname (if available and not blank)
 * 3. Agent's full name (null when the agent's personal information is hidden)
 *
 * @param isMultiThreadEnabled A flag indicating whether multi-threading is enabled.
 * @return The thread name, agent's nickname, or agent's full name (in priority order),
 *         or `null` if none is available or the agent's name is hidden.
 */
internal fun ChatThread.threadOrAgentName(isMultiThreadEnabled: Boolean): String? =
    threadName.takeIf { isMultiThreadEnabled }?.takeIf { it.isNotBlank() }
        ?: threadAgent?.nickname?.takeIf { it.isNotBlank() }
        ?: threadAgent?.fullName
