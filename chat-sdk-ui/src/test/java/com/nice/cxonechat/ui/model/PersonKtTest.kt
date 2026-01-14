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

package com.nice.cxonechat.ui.model

import com.nice.cxonechat.ui.domain.model.removeDefaultImageUrl
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PersonKtTest {
    @Test
    fun removeDefaultImageUrl_removes_expected_urls() {
        assertNull(
            removeDefaultImageUrl(
                """https://assets-qa.brandembassy.com/platform/static/public/img/user/l.png"""
            )
        )
        assertNull(
            removeDefaultImageUrl(
                """https://app-de-na1.niceincontact.com/img/user/t.png"""
            )
        )
        assertNull(
            removeDefaultImageUrl(
                """https://assets-qa.brandembassy.com/platform/static/public/img/user-default.png"""
            )
        )
    }

    @Test
    fun removeDefaultImageUrl_keeps_other_urls() {
        assertNotNull(
            removeDefaultImageUrl(
                """https://brand-embassy-avatars-qa.s3.eu-west-1.amazonaws.com/82d4eeac-4eb0-4ef2-8884-928c632c2275.jpg"""
            )
        )
        assertNotNull(removeDefaultImageUrl("""https://nice.com/my-image.png"""))
        assertNotNull(removeDefaultImageUrl("""https://nice.com/image/t.png"""))
        assertNotNull(removeDefaultImageUrl("""https://app-de-na1.niceincontact.com/user/user-default.png"""))
    }
}
