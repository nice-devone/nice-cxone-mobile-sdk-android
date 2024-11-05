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

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import kotlinx.serialization.encodeToString
import java.util.Date
import java.util.UUID

internal class PreferencesValueStorage(private val sharedPreferences: SharedPreferences) : ValueStorage {

    override var authToken: String?
        get() = sharedPreferences.getString(PREF_AUTH_TOKEN, null)
        set(value) = sharedPreferences.edit {
            putString(PREF_AUTH_TOKEN, value)
        }

    override var authTokenExpDate: Date?
        get() = sharedPreferences.getLong(PREF_AUTH_TOKEN_EXP_DATE, -1)
            .takeUnless { it == -1L }
            ?.let(::Date)
        set(value) = sharedPreferences.edit {
            if (value != null) {
                putLong(PREF_AUTH_TOKEN_EXP_DATE, value.time)
            } else {
                remove(PREF_AUTH_TOKEN_EXP_DATE)
            }
        }

    override var customerId: String?
        get() = sharedPreferences.getString(PREF_CUSTOMER_ID, null)
        set(value) = sharedPreferences.edit {
            putString(PREF_CUSTOMER_ID, value)
        }

    override var visitorId: UUID
        get() = sharedPreferences.getUUID(PREF_VISITOR_ID) ?: UUID.randomUUID().also {
            visitorId = it
        }
        set(value) = sharedPreferences.edit {
            putString(PREF_VISITOR_ID, value.toString())
        }

    override var visitDetails: VisitDetails?
        get() = sharedPreferences.getString(PREF_VISIT_DETAILS, null)?.let {
            Default.serializer.decodeFromString(it)
        }
        set(value) = sharedPreferences.edit {
            putString(PREF_VISIT_DETAILS, value?.let { Default.serializer.encodeToString(it) })
        }

    override val visitId: UUID
        get() = (
                visitDetails ?: VisitDetails().also {
                    visitDetails = it
                }
        ).visitId

    override val visitValidUntil: Date?
        get() = visitDetails?.validUntil

    override val destinationId: UUID = UUID.randomUUID()
    override var welcomeMessage: String
        get() = sharedPreferences.getStringOrEmpty(PREF_WELCOME_MESSAGE)
        set(value) = sharedPreferences.edit {
            putString(PREF_WELCOME_MESSAGE, value)
        }

    override var deviceToken: String?
        get() = sharedPreferences.getString(PREF_DEVICE_TOKEN, null)
        set(value) = sharedPreferences.edit {
            putString(PREF_DEVICE_TOKEN, value)
        }

    constructor(context: Context) : this(
        EncryptedSharedPreferences.create(
            PREFERENCE_FILE,
            PREFERENCE_KEY_ALIAS,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    )

    override fun clearStorage() {
        sharedPreferences.edit {
            clear()
        }
    }

    private fun SharedPreferences.getStringOrEmpty(
        key: String,
        defValue: String? = null,
    ): String = getString(key, defValue).orEmpty()
    private fun SharedPreferences.getUUID(
        key: String,
        defValue: String? = null,
    ): UUID? = getString(key, defValue)?.let(UUID::fromString)

    private companion object {
        private const val PREFERENCE_FILE = "com.nice.cxonechat.secure"
        private const val PREFERENCE_KEY_ALIAS = "com.nice.cxonechat.secure"
        private const val PREF_AUTH_TOKEN: String = "share_sdk_auth_token"
        private const val PREF_AUTH_TOKEN_EXP_DATE: String = "share_sdk_auth_token_exp_date"
        private const val PREF_VISITOR_ID: String = "share_visitor_id"
        private const val PREF_CUSTOMER_ID: String = "share_customer_id"
        private const val PREF_VISIT_DETAILS: String = "share_visit_details"
        private const val PREF_WELCOME_MESSAGE: String = "share_welcome_message"
        private const val PREF_DEVICE_TOKEN: String = "device_token"
    }
}
