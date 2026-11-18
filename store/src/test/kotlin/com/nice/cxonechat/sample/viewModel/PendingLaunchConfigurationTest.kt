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

internal class PendingLaunchConfigurationTest {

    private val pending = PendingLaunchConfiguration()

    @Test
    fun `nothing is pending until a request is offered`() {
        pending.consume() shouldBe null
    }

    @Test
    fun `an offered request is consumed exactly once`() {
        pending.offer(REQUEST)

        pending.consume() shouldBe REQUEST
        // A relaunch which carries no configuration must not silently reapply the previous one.
        pending.consume() shouldBe null
    }

    @Test
    fun `a second offer replaces a request which was never consumed`() {
        val replacement = REQUEST.copy(openChat = false)

        pending.offer(REQUEST)
        pending.offer(replacement)

        pending.consume() shouldBe replacement
    }

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
