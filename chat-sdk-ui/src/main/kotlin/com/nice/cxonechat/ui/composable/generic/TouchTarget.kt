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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.ui.unit.dp

/**
 * Smallest comfortable tap target on any axis, from the Material accessibility guidance.
 *
 * Deliberately outside both any single component and the theme. It is a platform accessibility floor
 * rather than a value a design gets to choose, so a component can hold itself to it without taking a
 * dependency on `ChatTheme` — and `Space.clickableSize` takes its default from here so the two cannot
 * drift apart.
 */
internal val MinTouchTarget = 48.dp
