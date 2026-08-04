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

    /**
     * Display an unbeatable error message in the UI. DISCONNECT the SDK.
     * Reads [com.nice.cxonechat.ui.data.ChatErrorState.message] and
     * [com.nice.cxonechat.ui.data.ChatErrorState.title] when non-null, falling back to the
     * generic localized resources. Callers must pass null for both values to avoid leaking
     * server-supplied text to users; the SDK enforces this by suppressing exception messages
     * before emitting this group.
     */
    HIGH,

    /**
     * Non-dismissible terminal error with a specific localized message. DISCONNECT the SDK.
     * Same severity as [HIGH], but reads [com.nice.cxonechat.ui.data.ChatErrorState.message] and
     * [com.nice.cxonechat.ui.data.ChatErrorState.title] when non-null, falling back to
     * [com.nice.cxonechat.ui.R.string.error_sdk_version_not_supported] for the body when message is null.
     * Currently used only for [com.nice.cxonechat.ChatState.SdkNotSupported].
     */
    HIGH_SPECIFIC,

    /** No UI message, just silently exit the chat. DISCONNECT the SDK. */
    SILENT_EXIT
}
