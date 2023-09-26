/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.reflect.KClass

/**
 * A repository to read a typed-object from a named Android file in the documents directory.
 *
 * @param Type Type of asset to read.
 * @property fileName Name of file to read/write.
 * @param type Class of asset to read.
 */
open class FileRepository<Type: Any>(
    private val fileName: String,
    type: KClass<Type>
) : Repository<Type>(type) {
    @Throws(RepositoryError::class)
    override fun doStore(string: String, context: Context) {
        try {
            context.openFileOutput(fileName, 0)?.use {
                doStore(string, it)
            }
        } catch(exc: IOException) {
            throw RepositoryError(TAG, "Error saving settings: $fileName:", exc)
        }
    }

    override fun doLoad(context: Context): String? {
        return try {
            return context.openFileInput(fileName).use {
                doLoad(it)
            }
        } catch (_: FileNotFoundException) {
            null
        } catch (exc: IOException) {
            throw RepositoryError(TAG, "Error loading settings: $fileName:", exc)
        }
    }

    override fun doClear(context: Context) {
        context.deleteFile(fileName)
    }

    companion object {
        private const val TAG = "FileRepository"
    }
}
