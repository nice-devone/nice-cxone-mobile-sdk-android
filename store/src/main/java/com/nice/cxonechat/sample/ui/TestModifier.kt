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

package com.nice.cxonechat.sample.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

/**
 * Extension function for the `Modifier` class that enables the use of test tags
 * as resource IDs in Compose UI testing. This is useful for integration with
 * external testing frameworks like Appium, which rely on resource IDs for element identification.
 *
 * @return A `Modifier` with semantics configured to use test tags as resource IDs.
 */
internal fun Modifier.tagsAsResourceIds() = semantics {
    testTagsAsResourceId = true
}

/**
 * Singleton object that provides a `Modifier` with the `tagsAsResourceIds` semantics applied.
 * This can be used as a shorthand to apply the test tag semantics globally.
 */
internal object TestModifier : Modifier by Modifier.Companion.tagsAsResourceIds()
