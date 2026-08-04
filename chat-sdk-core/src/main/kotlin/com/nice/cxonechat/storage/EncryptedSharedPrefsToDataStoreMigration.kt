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

@file:Suppress(
    "DEPRECATION" // Deprecated EncryptedSharedPreferences are used for migration purposes only - remove in future SDK version
)

package com.nice.cxonechat.storage

import android.content.Context
import android.content.SharedPreferences.Editor
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Custom migration for EncryptedSharedPreferences to DataStore.
 */
internal class EncryptedSharedPrefsToDataStoreMigration(
    private val context: Context,
    private val sharedPrefsName: String,
) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = withContext(Dispatchers.IO) {
        val sharedPrefs = getEncryptedSharedPreferences()
        sharedPrefs.all.isNotEmpty()
    }

    override suspend fun migrate(currentData: Preferences): Preferences = withContext(Dispatchers.IO) {
        val sharedPrefs = getEncryptedSharedPreferences()
        val mutablePrefs = currentData.toMutablePreferences()
        val aead = getAeadForMigration(context)
        for ((key, value) in sharedPrefs.all.entries) {
            if (key != null && value != null) {
                val valueStr = when (value) {
                    is String -> value
                    is Long, is Int, is Float, is Boolean -> value.toString()
                    else -> null
                }
                if (valueStr != null) {
                    val encryptedValueForStore = encryptForStore(aead, valueStr)
                    mutablePrefs[stringPreferencesKey(key)] = encryptedValueForStore
                }
            }
        }
        mutablePrefs
    }

    override suspend fun cleanUp() {
        val sharedPrefs = getEncryptedSharedPreferences()
        sharedPrefs.edit(action = Editor::clear)
    }

    private fun getEncryptedSharedPreferences() = EncryptedSharedPreferences.create(
        sharedPrefsName,
        sharedPrefsName,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun getAeadForMigration(context: Context): Aead {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_PREF_FILE, KEYSET_ALIAS)
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("${AndroidKeystoreKmsClient.PREFIX}$KEYSET_ALIAS")
            .build()
            .keysetHandle
        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun encryptForStore(aead: Aead, value: String): String {
        val ciphertext = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
        return android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
    }
}
