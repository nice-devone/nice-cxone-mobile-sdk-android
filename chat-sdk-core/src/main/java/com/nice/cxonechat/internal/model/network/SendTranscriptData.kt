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

package com.nice.cxonechat.internal.model.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SendTranscriptData(
    /** The contact information of the consumer on the external platform. */
    @SerialName("consumerContact") val consumerContact: ConsumerContact? = ConsumerContact(),
    /** The recipients to whom the transcript will be sent on the external platform. */
    @SerialName("consumerRecipients") val consumerRecipients: List<ConsumerRecipients> = emptyList(),
) {

    @Serializable
    data class ConsumerContact(
        /** The identifier of the contact on the external platform. */
        @SerialName("id") val id: String? = null,
    )

    @Serializable
    data class ConsumerRecipients(
        /** The identifier of the recipient on the external platform. */
        @SerialName("idOnExternalPlatform") val idOnExternalPlatform: String? = null,
    )
}
