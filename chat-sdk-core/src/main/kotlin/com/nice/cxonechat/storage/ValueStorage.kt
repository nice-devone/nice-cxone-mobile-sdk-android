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

package com.nice.cxonechat.storage

import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.serializer.InstantAsNumber
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Abstraction for classes providing persistent storage of provided variables.
 */
@Suppress("ComplexInterface")
internal interface ValueStorage {
    @Serializable
    data class VisitDetails(
        @SerialName("visitId")
        @Contextual
        val visitId: UUID = UUIDProvider.next(),
        @SerialName("validUntil")
        val validUntil: InstantAsNumber = Clock.System.now() + 30.minutes,
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
    var authTokenExpDate: Instant?

    /**
     * Stores the current transaction token model for secured session authentication.
     * This model contains the access token, expiration, and related identity information.
     * Default value is null. Used only when SecuredSessions feature is enabled.
     */
    var transactionTokenModel: TransactionTokenModel?

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
    val visitValidUntil: Instant?

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

    /**
     * Atomically checks whether a Satisfaction Survey has already been delivered (emitted to the
     * app) for the thread identified by [threadId] and, if not, records it as delivered. Mode-agnostic
     * - applies to `SingleThread`, `MultiThread`, and `LiveChat` alike. Returns `true` if this call is
     * the one that recorded the delivery, `false` if it was already recorded (by this or a concurrent
     * call).
     *
     * Delivery is tracked per thread rather than per survey message: a session only ever produces one
     * survey, so the thread id alone is a sufficient dedup key. Bounded in size — the oldest recorded
     * thread is evicted first.
     *
     * The check-and-record must happen atomically: [popupFlow][com.nice.cxonechat.ChatThreadActionHandler.popupFlow]
     * is a cold flow that re-subscribes to the socket on every collection, so more than one collector
     * can race to deliver the same survey.
     */
    fun markSurveyDeliveredIfAbsent(threadId: UUID): Boolean

    /**
     * Non-mutating check for whether a Satisfaction Survey was already delivered for [threadId].
     * Unlike [markSurveyDeliveredIfAbsent], never records a delivery itself.
     */
    fun isSurveyDelivered(threadId: UUID): Boolean

    suspend fun clearStorage()
}
