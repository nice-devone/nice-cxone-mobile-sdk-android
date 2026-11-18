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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue

/**
 * The anchors a sheet may rest at, which Material 3 requires each sheet to state for itself.
 *
 * Material 3 used to derive the half-height stop from layout conditions and let callers suppress it with
 * a `skipPartiallyExpanded` flag. From 1.5.0 the caller declares the set instead.
 *
 * Every sheet here declares [FullHeightOnly], so none of them offers a half-height stop. Adding
 * `PartiallyExpanded` is not a free extra option: the sheet then opens into that state, and Material 3 keys
 * its dismiss behaviour off `currentValue`, so a gesture that would have closed the sheet collapses it
 * instead. For content shorter than half the screen the two stops coincide, so this is invisible until
 * something tries to dismiss the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
internal object SheetAnchors {

    /** Open or closed, with no half-height stop. Replaces `skipPartiallyExpanded = true`. */
    val FullHeightOnly: Set<SheetValue> = setOf(SheetValue.Hidden, SheetValue.Expanded)
}
