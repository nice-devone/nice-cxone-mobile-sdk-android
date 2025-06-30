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

package com.nice.cxonechat.ui.domain

/**
 * Enum class representing lifecycle of a an popup.
 *
 * This enum is used to track the state or interaction with a popup, such as whether
 * it was displayed, clicked, or the result of an operation (success or failure).
 */
internal enum class ReportOnPopupAction {
    /**
     * Indicates that the popup was displayed to the user.
     */
    Displayed,

    /**
     * Indicates that the popup was clicked by the user.
     */
    Clicked,

    /**
     * Indicates that an operation related to the popup was successful.
     */
    Success,

    /**
     * Indicates that an operation related to the popup failed.
     */
    Failure
}
