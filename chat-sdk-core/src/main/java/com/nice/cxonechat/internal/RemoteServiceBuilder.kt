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

import com.nice.cxonechat.api.BaseRetrofitServiceBuilder
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.api.RemoteServiceCaching
import retrofit2.Retrofit
import retrofit2.create

internal class RemoteServiceBuilder : BaseRetrofitServiceBuilder<RemoteService>() {
    override fun self() = this

    override fun build(): RemoteService {
        val connection = requireNotNull(connection) { "Connection needs to be set, before build() is called." }
        var service: RemoteService = Retrofit.Builder()
            .client(buildClient())
            .baseUrl(connection.environment.chatUrl)
            .addConverterFactory(jsonConverterFactory())
            .build()
            .create()
        service = RemoteServiceCaching(service)
        return service
    }
}
