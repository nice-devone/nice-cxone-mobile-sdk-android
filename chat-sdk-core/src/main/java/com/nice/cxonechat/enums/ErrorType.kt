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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Definition of all errors reported by the server.
 */
@Serializable
internal enum class ErrorType(val value: String) {

    @SerialName("ConsumerReconnectionFailed")
    ConsumerReconnectionFailed("ConsumerReconnectionFailed"),

    @SerialName("TokenRefreshingFailed")
    TokenRefreshingFailed("TokenRefreshingFailed"),

    @SerialName("SendingMessageFailed")
    SendingMessageFailed("SendingMessageFailed"),

    @SerialName("RecoveringLivechatFailed")
    RecoveringLivechatFailed("RecoveringLivechatFailed"),

    @SerialName("RecoveringThreadFailed")
    RecoveringThreadFailed("RecoveringThreadFailed"),

    @SerialName("SendingOutboundFailed")
    SendingOutboundFailed("SendingOutboundFailed"),

    @SerialName("UpdatingThreadFailed")
    UpdatingThreadFailed("UpdatingThreadFailed"),

    @SerialName("ArchivingThreadFailed")
    ArchivingThreadFailed("ArchivingThreadFailed"),

    @SerialName("SendingTranscriptFailed")
    SendingTranscriptFailed("SendingTranscriptFailed"),

    @SerialName("SendingOfflineMessageFailed")
    SendingOfflineMessageFailed("SendingOfflineMessageFailed"),

    @SerialName("MetadataLoadFailed")
    MetadataLoadFailed("MetadataLoadFailed"),

    @SerialName("S3EventLoadFailed")
    S3EventLoadFailed("S3EventLoadFailed"),
}
