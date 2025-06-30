/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * A utility class for managing key-value storage using Android's DataStore.
 *
 * @param context The application context used to access the DataStore.
 */
@Single
internal class ValueStorage(
    private val context: Context,
) {

    // Extension property to initialize the DataStore instance.
    private val Context.storage: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_FILE_NAME)
    private val data: Flow<Preferences> by lazy { context.storage.data }

    /**
     * Retrieves a string value from the DataStore.
     *
     * @param key The key associated with the string value.
     * @return A [Flow] emitting the string value.
     */
    fun getString(key: StringKey): Flow<String> {
        return data.map { preferences: Preferences ->
            preferences[key.value].orEmpty()
        }
    }

    /**
     * Stores a string value in the DataStore.
     *
     * @param key The key to associate with the string value.
     * @param value The string value to store.
     */
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

    /**
     * Adds a string value to a set stored in the DataStore.
     *
     * @param key The key associated with the string set.
     * @param value The string value to add to the set.
     */
    suspend fun addStringToSet(key: StringSetKey, value: String) {
        context.storage.edit { preferences ->
            val currentValue = preferences[key.value].orEmpty()
            preferences[key.value] = currentValue + value
        }
    }

    /**
     * Retrieves a set of strings from the DataStore.
     *
     * @param key The key associated with the string set.
     * @return A [Flow] emitting the set of strings.
     */
    fun getStringSet(key: StringSetKey): Flow<Set<String>> {
        return data.map { preferences ->
            preferences[key.value].orEmpty()
        }
    }

    /**
     * Clears all stored preferences in the DataStore.
     */
    suspend fun clear() {
        context.storage.edit(MutablePreferences::clear)
    }

    internal companion object {
        private const val PREFERENCES_FILE_NAME = "com.nice.cxonechat.ui.settings"
        private const val PREF_REQUESTED_PERMISSIONS: String = "ui_requested_permissions"
        private const val PREF_DISMISSED_NOTIFICATIONS: String = "ui_dismissed_notifications"
        private const val STRING_SET_DELIMITER = ", "
        suspend fun ValueStorage.getStringSet(key: StringKey): Set<String> {
            return getString(key)
                .firstOrNull()
                .orEmpty()
                .split(STRING_SET_DELIMITER)
                .filterNot(String::isNullOrEmpty)
                .toSet()
        }

        suspend fun ValueStorage.setStringSet(key: StringKey, value: Set<String>) {
            setString(
                key = key,
                value = value.takeUnless(Set<String>::isEmpty)
                    ?.filterNot(String::isEmpty)
                    ?.joinToString(STRING_SET_DELIMITER)
                    .orEmpty()
            )
        }

        suspend fun ValueStorage.removeFromStringSet(key: StringKey, value: Set<String>) {
            val noEmptySet = value.filterNot(String::isEmpty).toSet()
            setStringSet(key, getStringSet(key).subtract(noEmptySet))
        }
    }

    /**
     * Enum representing keys for string values in the DataStore.
     *
     * @property value The [Preferences.Key] associated with the string value.
     */
    enum class StringKey(val value: Key<String>) {
        RequestedPermissionsKey(stringPreferencesKey(PREF_REQUESTED_PERMISSIONS))
    }

    /**
     * Enum representing keys for string sets in the DataStore.
     *
     * @property value The [Preferences.Key] associated with the string set.
     */
    enum class StringSetKey(val value: Key<Set<String>>) {
        DismissedNotificationsKey(stringSetPreferencesKey(PREF_DISMISSED_NOTIFICATIONS)),
    }
}
