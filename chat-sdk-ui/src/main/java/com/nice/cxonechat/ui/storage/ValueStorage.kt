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

package com.nice.cxonechat.ui.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.Preferences.Key
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single
internal class ValueStorage(
    private val context: Context,
) {

    private val Context.storage: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_FILE_NAME)
    private val data: Flow<Preferences> by lazy { context.storage.data }

    fun getString(key: StringKey): Flow<String> {
        return data.map { preferences: Preferences ->
            preferences[key.value].orEmpty()
        }
    }

    suspend fun setString(key: StringKey, value: String) {
        context.storage.edit { preferences ->
            preferences[key.value] = value
        }
    }

    suspend fun setStrings(
        map: Map<StringKey, String>,
    ) {
        context.storage.edit { preferences ->
            for ((key, value) in map) {
                preferences[key.value] = value
            }
        }
    }

    suspend fun clear() {
        context.storage.edit(MutablePreferences::clear)
    }

    private companion object {
        private const val PREFERENCES_FILE_NAME = "com.nice.cxonechat.ui.settings"
        private const val PREF_CUSTOM_VALUES: String = "share_custom_values_serialized"
        private const val PREF_REQUESTED_PERMISSIONS: String = "ui_requested_permissions"
    }

    enum class StringKey(val value: Key<String>) {
        CustomerCustomValuesKey(stringPreferencesKey(PREF_CUSTOM_VALUES)),
        RequestedPermissionsKey(stringPreferencesKey(PREF_REQUESTED_PERMISSIONS))
    }
}
