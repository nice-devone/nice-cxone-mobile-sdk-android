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

package com.nice.cxonechat.ui

import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import org.junit.Before
import org.junit.Rule

/**
 * Common setup for UI tests enabling accessibility checks.
 */
@RequiresApi(34)
abstract class AbstractComponentActivityUiTest {

    val checkFromRootView = true

    val accessibilityValidator: AccessibilityValidator = AccessibilityValidator()
        .apply {
            setRunChecksFromRootView(checkFromRootView)
            setSaveImages(true, true)
            setThrowExceptionFor(AccessibilityCheckResultType.WARNING)
        }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        // Enable accessibility checks with custom configuration:
        composeTestRule.enableAccessibilityChecks(accessibilityValidator)
    }

    protected fun getString(
        @StringRes resId: Int,
        vararg formatArgs: Any?,
    ): String = composeTestRule.activity.getString(resId, *formatArgs)

    protected fun Attachment.mimeTypeToDescription(): String {
        val type = mimeType.orEmpty()
        return when {
            type.startsWith("image/", ignoreCase = true) -> getString(string.content_description_attachment_type_image)
            type.startsWith("video/", ignoreCase = true) -> getString(string.content_description_attachment_type_video)
            type.startsWith("application/pdf", ignoreCase = true) -> getString(string.content_description_attachment_type_document)
            else -> getString(string.content_description_attachment_type_file)
        }
    }
}
