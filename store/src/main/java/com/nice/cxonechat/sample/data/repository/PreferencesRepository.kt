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
import androidx.core.content.edit
import kotlin.reflect.KClass

/**
 * A repository to read a typed-object from shared preferences.
 *
 * @param Type Type of asset to read.
 * @param key Preference key to target.
 * @param type Class of asset to read.
 * @param fileName Name of preference file to use.  Defaults to "com.nice.cxonechat.storefront".
 */
open class PreferencesRepository<Type: Any>(
    private val key: String,
    type: KClass<Type>,
    private val fileName: String = defaultFileName,
) : Repository<Type>(type) {
    /** Preferences object to access. */
    private val Context.preferences get() = getSharedPreferences(fileName, Context.MODE_PRIVATE)

    override fun doStore(string: String, context: Context) {
        context.preferences.edit {
            putString(key, string)
        }
    }

    override fun doLoad(context: Context) = context.preferences.getString(key, null)

    override fun doClear(context: Context) = context.preferences.edit {
        remove(key)
    }

    companion object {
        /** default preference file name. */
        const val defaultFileName = "com.nice.cxonechat.storefront"
    }
}
