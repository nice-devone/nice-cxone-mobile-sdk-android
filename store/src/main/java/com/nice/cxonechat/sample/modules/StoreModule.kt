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

package com.nice.cxonechat.sample.modules

import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerAndroid
import com.nice.cxonechat.log.ProxyLogger
import com.nice.cxonechat.sample.data.repository.ExtraCustomFieldRepository
import com.nice.cxonechat.sample.utilities.logging.FirebaseLogger
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.nice.cxonechat.sample")
internal class StoreModule {
    @Single
    fun provideChatInstanceProvider() = ChatInstanceProvider.get()

    @Single
    fun provideLogger(): Logger = ProxyLogger(
        FirebaseLogger(),
        LoggerAndroid("SampleApp")
    )

    @Single
    @Named("customer")
    fun provideCustomerFieldProvider(
        extraCustomFieldRepository: ExtraCustomFieldRepository,
    ): UiCustomFieldsProvider = object : UiCustomFieldsProvider {
        override fun customFields(): Map<String, String> = extraCustomFieldRepository.load().customerCustomFields
    }

    @Single
    @Named("contact")
    fun provideContactFieldProvider(
        extraCustomFieldRepository: ExtraCustomFieldRepository,
    ): UiCustomFieldsProvider = object : UiCustomFieldsProvider {
        override fun customFields(): Map<String, String> = extraCustomFieldRepository.load().contactCustomFields
    }
}
