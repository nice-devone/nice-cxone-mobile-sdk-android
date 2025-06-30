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

package com.nice.cxonechat.ui.domain.model

import com.nice.cxonechat.thread.ChatThread

/**
 * Extension function for `ChatThread` that retrieves the thread name or agent's full name.
 *
 * @param isMultiThreadEnabled A flag indicating whether multi-threading is enabled.
 * @return The thread name if multi-thread mode is enabled and the name is not empty;
 *         otherwise, the agent's full name, or `null` if neither is available.
 */
internal fun ChatThread.threadOrAgentName(isMultiThreadEnabled: Boolean): String? =
    threadName.takeIf { isMultiThreadEnabled }?.takeIf { it.isNotBlank() } ?: threadAgent?.fullName
