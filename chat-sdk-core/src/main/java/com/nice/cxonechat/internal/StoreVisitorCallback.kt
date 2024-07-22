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

import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class StoreVisitorCallback(
    private val logger: Logger,
) : Callback<Void>,
    LoggerScope by LoggerScope<StoreVisitorCallback>(logger) {

    override fun onResponse(call: Call<Void>, response: Response<Void>) = scope("onResponse") {
        if (response.isSuccessful) {
            logger.verbose("StoreVisitor created")
        } else {
            logger.warning("StoreVisitor creation failed: ${response.errorBody()?.string()}")
        }
    }

    override fun onFailure(call: Call<Void>, exception: Throwable) = scope("onFailure") {
        logger.warning("StoreVisitor creation failed", exception)
    }
}
