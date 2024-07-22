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

package com.nice.cxonechat.storage

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

/**
 * Simple implementation of [SharedPreferences] using a mutable map for in-memory storage.
 *
 * This implementation currently doesn't support queueing of edit actions or listening to changes in preferences.
 */
@Suppress(
    "FunctionMaxLength",
    "StringLiteralDuplication"
)
internal open class FakeSharedPreferences : SharedPreferences {

    private val map: MutableMap<String?, Any?> = mutableMapOf()

    override fun getAll(): MutableMap<String?, *> = map
        .toMutableMap()

    override fun getString(key: String?, defValue: String?): String? = map[key] as? String? ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = map[key]?.let {
        @Suppress("UNCHECKED_CAST")
        it as? MutableSet<String>?
    } ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int? ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long? ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float? ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean? ?: defValue

    override fun contains(key: String?): Boolean = map.contains(key)

    override fun edit(): Editor {
        /*
        Note: This editor implementation doesn't implement ordering of actions which should be applied to the stored preferences and also
        ignores commit/apply calls.
        In case used business logic will ever relly on those, they will have to be added to this implementation as well.
         */
        return object : Editor {
            override fun putString(key: String?, value: String?): Editor = put(key, value)

            override fun putStringSet(key: String?, values: MutableSet<String>?): Editor = put(key, values)

            override fun putInt(key: String?, value: Int): Editor = put(key, value)

            override fun putLong(key: String?, value: Long): Editor = put(key, value)

            override fun putFloat(key: String?, value: Float): Editor = put(key, value)

            override fun putBoolean(key: String?, value: Boolean): Editor = put(key, value)

            override fun remove(key: String?): Editor = apply {
                map.remove(key)
            }

            override fun clear(): Editor = apply {
                map.clear()
            }

            override fun commit(): Boolean {
                // no-op
                return true
            }

            override fun apply() {
                // no-op
            }

            private fun put(key: String?, value: Any?): Editor = apply {
                map[key] = value
            }
        }
    }

    override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        // unimplemented
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) {
        // unimplemented
    }
}
