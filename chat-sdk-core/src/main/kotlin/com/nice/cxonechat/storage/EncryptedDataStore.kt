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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException

private const val DATASTORE_NAME = "secure_store"
internal const val KEYSET_PREF_FILE = "cxonechat_tink_keyset"
internal const val KEYSET_ALIAS = "cxonechat_master_key"
private const val NULL_VALUE = "__NULL__"

/**
 * Contract interface so that tests can supply a FakeEncryptedDataStore.
 */
internal abstract class EncryptedDataStoreContract {
    abstract val dataStore: DataStore<Preferences>

    abstract fun getString(key: String): Flow<String?>
    abstract suspend fun putString(key: String, value: String?)
    abstract suspend fun remove(key: String)
    abstract suspend fun clear()

    protected abstract suspend fun encrypt(key: String, value: String): String
    protected abstract suspend fun decrypt(key: String, value: String): String
}

/**
 * Production implementation (real encryption).
 */
internal class EncryptedDataStore(
    private val context: Context,
    logger: Logger,
    override val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        migrations = listOf(
            EncryptedSharedPrefsToDataStoreMigration(context, PREFERENCE_FILE)
        ),
        produceFile = { File(context.filesDir, DATASTORE_FILE) }
    ),
) : EncryptedDataStoreContract(), LoggerScope by LoggerScope("EncryptedDataStore", logger) {
    private val aead: Aead

    init {
        tinkRegistered
        val keysetHandle = getOrCreateKeysetHandle(context)
        aead = keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    override fun getString(key: String): Flow<String?> =
        dataStore.data.map { prefs ->
            prefs[stringPreferencesKey(key)]?.let {
                val decrypted = decrypt(key, it)
                if (decrypted == NULL_VALUE) null else decrypted
            }
        }

    override suspend fun putString(key: String, value: String?) {
        val safeValue = value ?: NULL_VALUE
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = encrypt(key, safeValue)
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

    override suspend fun encrypt(key: String, value: String): String {
        return try {
            val ciphertext = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
            android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
        } catch (e: GeneralSecurityException) {
            scope("encrypt") {
                error("Encryption failed for key: $key", e)
                remove(key)
                NULL_VALUE
            }
        }
    }

    override suspend fun decrypt(key: String, value: String): String {
        return try {
            val bytes = android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
            val decrypted = aead.decrypt(bytes, null)
            String(decrypted, Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            scope("decrypt") {
                error("Decryption failed for key: $key", e)
                remove(key)
                NULL_VALUE
            }
        } catch (e: IllegalArgumentException) {
            scope("decrypt") {
                error("Decryption failed for key: $key", e)
                remove(key)
                NULL_VALUE
            }
        }
    }

    private fun getOrCreateKeysetHandle(context: Context): KeysetHandle {
        try {
            return AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_PREF_FILE, KEYSET_ALIAS)
                .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri("${AndroidKeystoreKmsClient.PREFIX}$KEYSET_ALIAS")
                .build()
                .keysetHandle
        } catch (e: GeneralSecurityException) {
            throw IllegalStateException(
                "Failed to initialize secure keyset. The Android Keystore may be unavailable, locked, or corrupted." +
                        "Please check device security settings and try again.",
                e
            )
        } catch (e: IOException) {
            throw IllegalStateException(
                "Failed to access keyset storage. There may be an issue with device storage or permissions.",
                e
            )
        }
    }

    companion object {
        private const val PREFERENCE_FILE = "com.nice.cxonechat.secure"
        private const val DATASTORE_FILE = "$DATASTORE_NAME.preferences_pb"
        private val tinkRegistered by lazy {
            AeadConfig.register()
            true
        }
    }
}
