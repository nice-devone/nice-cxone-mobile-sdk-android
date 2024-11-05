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

package com.nice.cxonechat.storage

import com.nice.cxonechat.internal.serializer.DateAsNumber
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

/**
 * Abstraction for classes providing persistent storage of provided variables.
 */
@Suppress("ComplexInterface")
internal interface ValueStorage {
    @Serializable
    data class VisitDetails(
        @SerialName("visitId")
        @Contextual
        val visitId: UUID = UUID.randomUUID(),
        @SerialName("validUntil")
        val validUntil: DateAsNumber = Date(Date().time + 30 * 60 * 1000)
    )

    /**
     * OAuth authorization token.
     * Default value is `null`.
     */
    var authToken: String?

    /**
     * Expiration date of [authToken], before which it should be refreshed.
     * Default value is null.
     */
    var authTokenExpDate: Date?

    /**
     * User application specific id.
     * Value is not writeable and remains the same for the time the app is
     * installed.
     */
    val visitorId: UUID

    /**
     * Details of the current visit id.  Contains the id itself and it's valid until date.
     */
    var visitDetails: VisitDetails?

    /**
     * The current visit id.
     *
     * Value should be created when the visit starts (ie., a page view event is generated *and*
     * no events have been generated for more than 30 minutes.)
     */
    val visitId: UUID

    /**
     * 30 minutes from the time the last page view event was generated.
     *
     * Used to create a new visit when there is a page view event and no other events have
     * been generated in the last 30 minutes.)
     */
    val visitValidUntil: Date?

    /**
     * Authorized user id.
     * Default value is null.
     */
    var customerId: String?

    /**
     * Connection session id.
     * Value is not writeable and remains the same for the duration of
     * session on this device.
     */
    val destinationId: UUID

    /**
     * Personalized welcome message.
     * Default value is empty string.
     */
    var welcomeMessage: String

    /**
     * Persisted deviceToken.
     * Default value is empty string.
     */
    var deviceToken: String?

    fun clearStorage()
}
