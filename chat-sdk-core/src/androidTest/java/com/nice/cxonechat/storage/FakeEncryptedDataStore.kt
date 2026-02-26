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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class FakeEncryptedDataStore(
    override val dataStore: DataStore<Preferences>,
) : EncryptedDataStoreContract() {
    private companion object {
        const val NULL_VALUE = "__NULL__"
    }

    override fun getString(key: String): Flow<String?> =
        dataStore.data.map { prefs ->
            val value = prefs[stringPreferencesKey(key)]
            if (value == NULL_VALUE) null else value
        }

    override suspend fun putString(key: String, value: String?) {
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = value ?: NULL_VALUE
        }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    override suspend fun encrypt(key: String, value: String) = value
    override suspend fun decrypt(key: String, value: String) = value
}
