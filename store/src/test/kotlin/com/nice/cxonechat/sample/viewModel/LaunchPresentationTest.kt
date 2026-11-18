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

package com.nice.cxonechat.sample.viewModel

import com.nice.cxonechat.sample.data.models.RequestedEnvironment
import com.nice.cxonechat.sample.data.models.SdkConfigurationRequest
import com.nice.cxonechat.sample.data.models.StoreLaunchRequest
import io.kotest.matchers.shouldBe
import org.junit.Test

internal class LaunchPresentationTest {

    @Test
    fun `the storefront starts hidden when a launch request is already waiting`() {
        presentation(pending = true).isStorefrontVisible.value shouldBe false
    }

    @Test
    fun `the storefront starts visible on an ordinary launch`() {
        presentation(pending = false).isStorefrontVisible.value shouldBe true
    }

    @Test
    fun `requesting the chat keeps the storefront unbuilt`() {
        val presentation = presentation(pending = true)

        presentation.requestOpenChat()

        presentation.isOpenChatPending shouldBe true
        presentation.isStorefrontVisible.value shouldBe false
    }

    @Test
    fun `a pending chat request is consumed exactly once`() {
        val presentation = presentation(pending = true)
        presentation.requestOpenChat()

        presentation.consumeOpenChatRequest() shouldBe true
        // Recreating the activity while OpeningChat is current must not open a second chat.
        presentation.consumeOpenChatRequest() shouldBe false
    }

    @Test
    fun `returning from the chat builds the storefront`() {
        val presentation = presentation(pending = true)
        presentation.requestOpenChat()

        presentation.showStorefront()

        presentation.isStorefrontVisible.value shouldBe true
    }

    private fun presentation(pending: Boolean) = LaunchPresentation(
        PendingLaunchConfiguration().apply { if (pending) offer(REQUEST) }
    )

    private companion object {
        val REQUEST = StoreLaunchRequest(
            configuration = SdkConfigurationRequest(
                environment = RequestedEnvironment.Named("PROD_EU1"),
                brandId = 1234L,
                channelId = "chat_abc",
            ),
            loginData = null,
            openChat = true,
        )
    }
}
