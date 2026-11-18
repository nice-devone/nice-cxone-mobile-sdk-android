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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass

/**
 * Whether the window is in Material's compact height class, as a phone held in landscape is.
 *
 * Height rather than orientation, so a landscape tablet, whose window still has room above a keyboard, does
 * not count. Read from the window rather than the device, for the reasons [isLandscapeWindow] gives.
 */
@Composable
internal fun isCompactHeightWindow(): Boolean =
    LocalWindowInfo.current.containerDpSize.height < WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp
