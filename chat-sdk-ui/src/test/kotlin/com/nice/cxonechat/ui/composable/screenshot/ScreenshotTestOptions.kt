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

package com.nice.cxonechat.ui.composable.screenshot

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziOptions.CompareOptions

/**
 * Shared Roborazzi options for all screenshot tests.
 *
 * `maxDistance` with `vShift`/`hShift` absorbs anti-aliasing noise between Linux CI and dev machines. The
 * threshold is how much of the image may differ.
 * Raise it only with a measurement, and prefer a per-class override and document the justification for any increase.
 */
internal val screenshotRoborazziOptions = RoborazziOptions(
    compareOptions = CompareOptions(
        changeThreshold = 0.0005F, // 0.05% of pixels may differ
        imageComparator = SimpleImageComparator(
            maxDistance = 0.007F, // 0.7% of pixel color distance may differ
            vShift = 2,
            hShift = 2,
        ),
    ),
)

/**
 * How long a screenshot test waits for content that resolves off the main thread before capturing.
 *
 * Generous on purpose: it only ever has to cover one dispatcher hop, but the first case in a class also pays
 * the Compose and Robolectric warm-up behind it. Shared by the classes waiting for a file's extension to
 * resolve, so that one bound covers them all. `ChatConversationScreenshotTest` keeps a bound of its own, for a
 * different hop and with its own measurements behind it; the two are the same number today by coincidence.
 */
internal const val SETTLE_TIMEOUT_MILLIS = 60_000L
