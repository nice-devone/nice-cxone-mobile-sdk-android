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
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.HttpUrl
import java.io.File
import java.io.InputStream
import java.io.OutputStream

private val DOMAIN_REGEX = Regex("""(?i)domain=\s*([^;\s]+)\s*""")

internal object CookieAsStringSerializer : KSerializer<Cookie> {

    override val descriptor =
        PrimitiveSerialDescriptor("Cookie", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Cookie) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Cookie {
        val cookieStr = decoder.decodeString()

        // Extract domain from cookie string if present
        val domain = DOMAIN_REGEX
            .find(cookieStr)
            ?.groupValues
            ?.get(1)
            ?.trimStart('.')

        val url = HttpUrl.Builder()
            .scheme("https")
            .host(domain ?: "localhost")
            .build()

        return Cookie.parse(url, cookieStr)
            ?: throw SerializationException("Invalid cookie: $cookieStr")
    }
}

private class EncryptedCookieListSerializer(
    private val aead: Aead,
    private val logger: Logger = LoggerNoop,
) : Serializer<List<Cookie>> {

    override val defaultValue: List<Cookie> = emptyList()

    private val json = Json { ignoreUnknownKeys = true }

    private val listSerializer =
        ListSerializer(CookieAsStringSerializer)

    override suspend fun readFrom(input: InputStream): List<Cookie> {
        return runCatching {
            val encrypted = input.readBytes()
            if (encrypted.isEmpty()) return emptyList()
            val decrypted = aead.decrypt(encrypted, null).decodeToString()
            json.decodeFromString(listSerializer, decrypted)
        }.onFailure { e ->
            logger.log(Level.Error, "Unexpected error while reading cookie data", e)
        }.getOrNull() ?: emptyList()
    }

    override suspend fun writeTo(
        t: List<Cookie>,
        output: OutputStream,
    ) {
        runCatching {
            val encoded = json.encodeToString(listSerializer, t)
            val encrypted = aead.encrypt(encoded.toByteArray(), null)
            output.write(encrypted)
        }.onFailure { e ->
            logger.log(Level.Error, "Failed to write encrypted cookie data", e)
        }
    }
}

internal class EncryptedCookieDataStore private constructor(
    private val dataStore: DataStore<List<Cookie>>,
) {
    /**
     * Saves cookies to the encrypted data store.
     * This is a suspend function to properly handle DataStore's asynchronous operations.
     *
     * @param url The URL associated with these cookies
     * @param cookies The cookies to save
     */
    suspend fun saveCookies(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        val now = System.currentTimeMillis()
        dataStore.updateData { stored ->
            val validStored = stored.filter { it.expiresAt == Long.MIN_VALUE || it.expiresAt > now }
            val updated = validStored.filterNot { storedCookie ->
                cookies.any { newCookie -> storedCookie.name == newCookie.name && storedCookie.matches(url) }
            } + cookies
            updated
        }
    }

    /**
     * Loads cookies from the encrypted data store that match the given URL.
     * This is a suspend function to properly handle DataStore's asynchronous operations.
     *
     * @param url The URL to match cookies against
     * @return List of cookies that match the URL and are not expired
     */
    suspend fun loadCookies(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return dataStore.data.first()
            .filter {
                (it.expiresAt == Long.MIN_VALUE || it.expiresAt > now) &&
                        it.matches(url)
            }
    }

    suspend fun clear() {
        dataStore.updateData { emptyList() }
    }

    companion object {
        private const val COOKIE_DATASTORE_FILE = "persistent_cookies.pb"
        private const val COOKIE_KEYSET_PREF_FILE = "cookie_datastore_keyset"
        private const val COOKIE_KEYSET_ALIAS = "cookie_master_key"

        suspend fun create(
            context: Context,
            logger: Logger = LoggerNoop,
        ): EncryptedCookieDataStore =
            withContext(Dispatchers.IO) {
                AeadConfig.register()

                val keysetHandle = AndroidKeysetManager.Builder()
                    .withSharedPref(
                        context,
                        COOKIE_KEYSET_PREF_FILE,
                        COOKIE_KEYSET_ALIAS
                    )
                    .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                    .withMasterKeyUri(
                        "${AndroidKeystoreKmsClient.PREFIX}$COOKIE_KEYSET_ALIAS"
                    )
                    .build()
                    .keysetHandle

                val aead = keysetHandle.getPrimitive(
                    RegistryConfiguration.get(),
                    Aead::class.java
                )

                val dataStore = DataStoreFactory.create(
                    serializer = EncryptedCookieListSerializer(aead, logger),
                    corruptionHandler = ReplaceFileCorruptionHandler {
                        emptyList()
                    },
                    produceFile = {
                        File(context.filesDir, COOKIE_DATASTORE_FILE)
                    }
                )

                EncryptedCookieDataStore(dataStore)
            }

        /**
         * Test-only factory for injecting a fake in-memory DataStore.
         */
        internal fun createForTest(dataStore: DataStore<List<Cookie>>): EncryptedCookieDataStore =
            EncryptedCookieDataStore(dataStore)
    }
}
