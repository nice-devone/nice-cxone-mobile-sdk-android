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
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.reflect.KClass

/**
 * A read-only repository to read a typed-object from a named Android asset.
 *
 * @param Type Type of asset to read.
 * @param name Name of asset to read.
 * @param type Class of asset to read.
 */
open class AssetRepository<Type : Any>(
    private val name: String,
    type: KClass<Type>,
) : Repository<Type>(type) {

    override fun doStore(string: String, context: Context) {
        throw RepositoryError("An attempt was made to write to asset: $name")
    }

    override fun doLoad(context: Context): String? =
        try {
            context.assets.open(name).use {
                doLoad(it)
            }
        } catch (_: FileNotFoundException) {
            null
        } catch (exc: IOException) {
            throw RepositoryError("Error loading settings: $name", exc)
        }

    override fun doClear(context: Context) {
        throw RepositoryError("An attempt was made to clear asset: $name")
    }
}
