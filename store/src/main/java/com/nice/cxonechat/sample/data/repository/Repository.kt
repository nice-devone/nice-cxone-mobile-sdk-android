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

package com.nice.cxonechat.sample.data.repository

import android.content.Context
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

/**
 * Abstract concept of a named place to store "complex" data.
 *
 * Data will be converted to JSON and then saved via the described mechanism.
 *
 * @param Type type of data to be stored.
 * @param type class of data to be stored.
 */
abstract class Repository<Type : Any>(
    private val type: KClass<Type>,
) {
    /**
     * Save an object using this repository.
     *
     * @param context Android [Context] to be used for resource resolution and/or file access.
     * @param item item to be saved.
     * @throws [SerializationException] if Json encounters an error.
     * @throws [Exception] rethrows any exception thrown by [doStore]
     */
    @Throws(SerializationException::class)
    open fun save(context: Context, item: Type?) {
        if (item != null) {
            doStore(toJson(item), context)
        } else {
            doClear(context)
        }
    }

    /**
     * Load an object using this repository.
     *
     * @param context Android [Context] to be used for resource resolution and/or file access.
     * @return item loaded.
     * @throws [SerializationException] if Json encounters an error.
     * @throws any exception thrown by [doLoad] will be rethrown.
     */
    @Throws(SerializationException::class)
    open fun load(context: Context) = doLoad(context)?.let(::fromJson)

    /**
     * Remove this repository.
     *
     * @param context Android [Context] to be used for resource resolution and/or file access.
     * @throws any exception thrown by [doClear] will be rethrown.
     */
    open fun clear(context: Context) = doClear(context)

    /**
     * Convenience function to store an already serialized object to an [OutputStream].
     *
     * @param string serialized data to be stored.
     * @param stream stream to save to.
     * @throws RepositoryError if any error is encountered while writing to the stream.
     */
    @Throws(RepositoryError::class)
    protected fun doStore(string: String, stream: OutputStream) {
        try {
            stream.write(string.toByteArray(Charsets.UTF_8))
        } catch (exc: IOException) {
            throw RepositoryError("Error encountered saving data", exc)
        }
    }

    /**
     * Convenience function to load an object from an [InputStream].
     *
     * @param stream [InputStream] to read.
     * @return object read from [stream] and parsed to [Type]
     * @throws RepositoryError if any error is encountered while reading or parsing the stream.
     */
    @Throws(RepositoryError::class)
    protected fun doLoad(stream: InputStream): String = try {
        stream.readBytes().toString(Charsets.UTF_8)
    } catch (exc: IOException) {
        throw RepositoryError("Error encountered loading data", exc)
    }

    /**
     * Store an object, in json string form, to this repository.
     *
     * @param string converted json object.
     * @param context Android [Context] to be used for resource resolution and/or file access.
     */
    abstract fun doStore(string: String, context: Context)

    /**
     * Read the contents of the repository as a String.
     *
     * @param context Android [Context] to be used for resource resolution and/or file access.
     * @return [String] contents of the repository or `null` if the repository is empty or does not exist.
     */
    abstract fun doLoad(context: Context): String?

    /**
     * Remove or erase the contents of the repository.
     *
     * @param context Android [Context] to be used for resource resolution and/or file access.
     */
    abstract fun doClear(context: Context)

    /**
     * Convert an item to Json for storage in the repository.
     *
     * @param item Data item to convert to Json.
     * @return item converted to Json as a [String].
     * @throws [SerializationException] if any error is encountered during the conversion.
     */
    private fun toJson(item: Type) = json.encodeToString(serializer = json.serializersModule.serializer(type.java), value = item)

    /**
     * Parse an item of type Type from a Json-encoded [String].
     *
     * @param string [String] to parse as Json.
     * @return object of type Type parsed from [string].
     * @throws [SerializationException] if any error is encountered during the conversion.
     */
    private fun fromJson(string: String): Type =
        json.decodeFromString(deserializer = json.serializersModule.serializer(type.java), string = string) as Type
}

private val json = Json {
    ignoreUnknownKeys = true
}
