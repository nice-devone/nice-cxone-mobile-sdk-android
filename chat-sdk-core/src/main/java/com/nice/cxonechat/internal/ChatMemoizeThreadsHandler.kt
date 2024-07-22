/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatThreadsHandler

/**
 * Implementation of the [ChatWithParameters] which assures that only one instance of [ChatThreadsHandler] is ever
 * created.
 *
 * It memorizes the first instance which is created and prevents further calls to the supplied
 * [ChatWithParameters.threads] method.
 */
internal class ChatMemoizeThreadsHandler(private val origin: ChatWithParameters) : ChatWithParameters by origin {

    private val chatThreadsHandlerMemoized by lazy(origin::threads)

    override fun threads(): ChatThreadsHandler = chatThreadsHandlerMemoized
}
