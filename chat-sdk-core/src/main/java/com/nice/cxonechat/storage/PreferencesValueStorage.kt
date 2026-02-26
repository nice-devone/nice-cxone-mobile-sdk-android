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

import android.content.Context
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID

internal class PreferencesValueStorage(
    context: Context,
    logger: Logger = LoggerNoop,
    internal val store: EncryptedDataStoreContract = EncryptedDataStoreProvider.getInstance(context, logger),
) : ValueStorage {

    override var authToken: String?
        get() = runBlocking {
            store.getString(PREF_AUTH_TOKEN).firstOrNull()
        }
        set(value) = runBlocking {
            store.putString(PREF_AUTH_TOKEN, value)
        }

    override var transactionTokenModel: TransactionTokenModel?
        get() = runBlocking {
            store.getString(PREF_TRANSACTION_TOKEN).firstOrNull()?.let { Default.serializer.decodeFromString(it) }
        }
        set(value) = runBlocking {
            if (value != null) {
                store.putString(PREF_TRANSACTION_TOKEN, Default.serializer.encodeToString(value))
            } else {
                store.remove(PREF_TRANSACTION_TOKEN)
            }
        }

    override var authTokenExpDate: Date?
        get() = runBlocking {
            store.getString(PREF_AUTH_TOKEN_EXP_DATE).firstOrNull()?.toLongOrNull()?.let(::Date)
        }
        set(value) = runBlocking {
            if (value != null) {
                store.putString(PREF_AUTH_TOKEN_EXP_DATE, value.time.toString())
            } else {
                store.remove(PREF_AUTH_TOKEN_EXP_DATE)
            }
        }

    override var customerId: String?
        get() = runBlocking {
            store.getString(PREF_CUSTOMER_ID).firstOrNull()
        }
        set(value) = runBlocking {
            value?.let { store.putString(PREF_CUSTOMER_ID, it) } ?: store.remove(PREF_CUSTOMER_ID)
        }

    override var visitorId: UUID
        get() = runBlocking {
            store.getString(PREF_VISITOR_ID).firstOrNull()?.let(UUID::fromString)
                ?: UUID.randomUUID().also { visitorId = it }
        }
        set(value) = runBlocking { store.putString(PREF_VISITOR_ID, value.toString()) }

    override var visitDetails: VisitDetails?
        get() = runBlocking {
            store.getString(PREF_VISIT_DETAILS).firstOrNull()?.let { Default.serializer.decodeFromString(it) }
        }
        set(value) = runBlocking {
            if (value != null) {
                store.putString(PREF_VISIT_DETAILS, Default.serializer.encodeToString(value))
            } else {
                store.remove(PREF_VISIT_DETAILS)
            }
        }

    override val visitId: UUID
        get() = visitDetails?.visitId ?: VisitDetails().also { visitDetails = it }.visitId

    override val visitValidUntil: Date?
        get() = visitDetails?.validUntil

    override val destinationId: UUID = UUID.randomUUID()
    override var welcomeMessage: String
        get() = runBlocking {
            store.getString(PREF_WELCOME_MESSAGE).firstOrNull().orEmpty()
        }
        set(value) = runBlocking { store.putString(PREF_WELCOME_MESSAGE, value) }

    override var deviceToken: String?
        get() = runBlocking {
            store.getString(PREF_DEVICE_TOKEN).firstOrNull()
        }
        set(value) = runBlocking {
            store.putString(PREF_DEVICE_TOKEN, value)
        }

    override fun clearStorage() {
        runBlocking { store.clear() }
    }

    private companion object {
        private const val PREF_AUTH_TOKEN: String = "share_sdk_auth_token"
        private const val PREF_TRANSACTION_TOKEN: String = "share_sdk_refresh_token"
        private const val PREF_AUTH_TOKEN_EXP_DATE: String = "share_sdk_auth_token_exp_date"
        private const val PREF_VISITOR_ID: String = "share_visitor_id"
        private const val PREF_CUSTOMER_ID: String = "share_customer_id"
        private const val PREF_VISIT_DETAILS: String = "share_visit_details"
        private const val PREF_WELCOME_MESSAGE: String = "share_welcome_message"
        private const val PREF_DEVICE_TOKEN: String = "device_token"
    }
}
