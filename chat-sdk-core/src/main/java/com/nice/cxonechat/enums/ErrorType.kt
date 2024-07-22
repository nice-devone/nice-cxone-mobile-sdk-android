/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.enums

import com.google.gson.annotations.SerializedName

/**
 * Definition of all errors reported by the server.
 */
internal enum class ErrorType(val value: String) {

    @SerializedName("ConsumerReconnectionFailed")
    ConsumerReconnectionFailed("ConsumerReconnectionFailed"),

    @SerializedName("TokenRefreshingFailed")
    TokenRefreshingFailed("TokenRefreshingFailed"),

    @SerializedName("SendingMessageFailed")
    SendingMessageFailed("SendingMessageFailed"),

    @SerializedName("RecoveringLivechatFailed")
    RecoveringLivechatFailed("RecoveringLivechatFailed"),

    @SerializedName("RecoveringThreadFailed")
    RecoveringThreadFailed("RecoveringThreadFailed"),

    @SerializedName("SendingOutboundFailed")
    SendingOutboundFailed("SendingOutboundFailed"),

    @SerializedName("UpdatingThreadFailed")
    UpdatingThreadFailed("UpdatingThreadFailed"),

    @SerializedName("ArchivingThreadFailed")
    ArchivingThreadFailed("ArchivingThreadFailed"),

    @SerializedName("SendingTranscriptFailed")
    SendingTranscriptFailed("SendingTranscriptFailed"),

    @SerializedName("SendingOfflineMessageFailed")
    SendingOfflineMessageFailed("SendingOfflineMessageFailed"),

    @SerializedName("MetadataLoadFailed")
    MetadataLoadFailed("MetadataLoadFailed"),

    S3EventLoadFailed("S3EventLoadFailed"),
}
