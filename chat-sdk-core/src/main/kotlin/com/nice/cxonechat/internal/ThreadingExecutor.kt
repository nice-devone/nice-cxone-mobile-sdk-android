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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

internal class ThreadingExecutor(
    override val coroutineScope: CoroutineScope,
    override val storageWriteScope: CoroutineScope,
    override val storageDispatcher: CoroutineDispatcher,
    override val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    override val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    override val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Threading {
    override val guards = Threading.Guards()
}
