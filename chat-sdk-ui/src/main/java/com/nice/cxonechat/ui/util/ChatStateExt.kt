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

import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Preparing

/**
 * Returns true if the chat state is `Prepared` or any state that follows it in the lifecycle
 * (e.g., `Connecting`, `Connected`, `Ready`), excluding error states (`Offline`, `ConnectionLost`, `SdkNotSupported`).
 */
internal fun ChatState.isAtLeastPrepared(): Boolean = this !== Initial && this !== Preparing
