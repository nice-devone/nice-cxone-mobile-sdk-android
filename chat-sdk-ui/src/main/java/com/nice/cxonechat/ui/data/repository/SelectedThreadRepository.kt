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

package com.nice.cxonechat.ui.data.repository

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Suppress("UseDataClass")
@Single
internal class SelectedThreadRepository {
    private val mutableChatThreadHandlerFlow: MutableStateFlow<ChatThreadHandler> = MutableStateFlow(NoThreadHandler)

    var chatThreadHandler: ChatThreadHandler
        get() = mutableChatThreadHandlerFlow.value
        set(value) {
            mutableChatThreadHandlerFlow.value = value
        }
    val chatThreadHandlerFlow = mutableChatThreadHandlerFlow.asStateFlow()
}
