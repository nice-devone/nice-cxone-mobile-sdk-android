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

package com.nice.cxonechat.ui.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val DATASTORE_FILE_NAME = "com.nice.cxonechat.ui.secure_settings"

// DataStore allows only one active instance per file for the whole process; declared once here
// (not as a member of EncryptedPendingSurveyStorage) so every instance of that class shares it,
// instead of each opening its own and crashing with "multiple DataStores active for the same file".
private val Context.pendingSurveyDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_FILE_NAME)

/**
 * Encrypted (AES-256-GCM, Android Keystore), backup-excluded storage for the Satisfaction Survey
 * background stash ([com.nice.cxonechat.ui.data.repository.SelectedThreadRepository.stashSurvey]/
 * [com.nice.cxonechat.ui.data.repository.SelectedThreadRepository.takeSurvey]).
 *
 * Deliberately separate from [ValueStorage] - that store is unencrypted and backup-eligible, which
 * is fine for its other keys (simple UI flags) but not for a survey URL, which can carry customer/
 * session identifiers. Backup exclusion operates on whole files, so the stash needs its own file to
 * be excludable without pulling those other, harmless keys out of backups too.
 *
 * Constructed by [ValueStorage] (not a standalone Koin dependency), which already receives the
 * [Context] this needs.
 */
internal class EncryptedPendingSurveyStorage(
    private val context: Context,
) {

    /** Exposed (not `private`) only so tests can assert the on-disk value is not the plaintext. */
    internal val valueKey = stringPreferencesKey(PREF_VALUE)
    internal val dataStore: DataStore<Preferences> get() = context.pendingSurveyDataStore

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Atomically reads, decrypts, transforms, re-encrypts and rewrites the stored value.
     *
     * `Dispatchers.IO`: the transform below touches the Keystore ([getOrCreateSecretKey]) - DataStore
     * does not dispatch `edit`'s transform off the caller's own dispatcher, so without this a caller on
     * `Dispatchers.Main` (e.g. a ViewModel's `withContext(NonCancellable)`, which only adds cancellation
     * protection, not a dispatcher switch) would run Keystore/disk I/O directly on the main thread.
     */
    suspend fun update(transform: (String) -> String) = withContext(Dispatchers.IO) {
        context.pendingSurveyDataStore.edit { preferences ->
            val current = preferences[valueKey]?.let(::decryptOrNull).orEmpty()
            preferences[valueKey] = encrypt(transform(current))
        }
        Unit
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        context.pendingSurveyDataStore.edit(MutablePreferences::clear)
        Unit
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey()) }
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + IV_DELIMITER + Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    // A corrupted or Keystore-invalidated value (e.g. the key was wiped by a factory-reset-protection
    // event) must not crash the caller - treat it the same as an absent/empty stash.
    private fun decryptOrNull(encoded: String): String? {
        val (ivPart, ciphertextPart) = encoded.split(IV_DELIMITER, limit = 2).takeIf { it.size == 2 } ?: return null
        return try {
            val iv = Base64.decode(ivPart, Base64.NO_WRAP)
            val ciphertext = Base64.decode(ciphertextPart, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
                .apply { init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)) }
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (expected: GeneralSecurityException) {
            null
        } catch (expected: IllegalArgumentException) {
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(AES_KEY_SIZE_BITS)
                    .build()
            )
        }.generateKey()
    }

    private companion object {
        const val PREF_VALUE = "ui_pending_satisfaction_surveys"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "com.nice.cxonechat.ui.pending_survey_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val AES_KEY_SIZE_BITS = 256
        const val IV_DELIMITER = ":"
    }
}
