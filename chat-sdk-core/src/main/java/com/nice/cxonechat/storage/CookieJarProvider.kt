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

package com.nice.cxonechat.storage

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop

internal object CookieJarProvider {
    @Volatile
    private var instance: PersistentCookieJar? = null

    fun getInstance(
        context: Context,
        testCookieDataStore: EncryptedCookieDataStore? = null,
        logger: Logger = LoggerNoop,
    ): PersistentCookieJar {
        return instance ?: synchronized(this) {
            instance ?: PersistentCookieJar(context.applicationContext, testCookieDataStore, logger).also {
                instance = it
            }
        }
    }

    /**
     * Clears the singleton instance. This method is intended for testing purposes only
     * to ensure test isolation by resetting the singleton state between tests.
     */
    @VisibleForTesting
    fun clearInstance() {
        synchronized(this) {
            instance = null
        }
    }
}
