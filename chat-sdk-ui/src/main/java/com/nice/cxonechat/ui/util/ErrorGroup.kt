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

package com.nice.cxonechat.ui.util

/**
 * Represents the severity group for errors.
 */
internal enum class ErrorGroup {

    /** Just log the warning, no UI message. */
    DO_NOTHING,

    /** Display a beatable error message in the UI. DO NOT disconnect the SDK. */
    LOW,

    /**
     * Special case for specific low severity errors where we want to show a specific message.
     */
    LOW_SPECIFIC,

    /** Display an unbeatable error message in the UI. DISCONNECT the SDK. */
    HIGH
}
