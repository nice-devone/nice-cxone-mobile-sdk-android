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

package com.nice.cxonechat.tool

import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.internal.ChatEntrails
import com.nice.cxonechat.internal.Threading
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.OkHttpClient

internal class ChatEntrailsMock(
    override val sharedClient: OkHttpClient,
    override val storage: ValueStorage,
    override val service: RemoteService,
    override val logger: Logger,
    override val environment: Environment,
) : ChatEntrails {

    override val threading: Threading = Threading.Identity
}
