package com.nice.cxonechat.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Date
import java.util.UUID

internal class PreferencesValueStorage(private val sharedPreferences: SharedPreferences) : ValueStorage {

    override var authToken: String?
        get() = sharedPreferences.getString(PREF_AUTH_TOKEN, null)
        set(value) = sharedPreferences.edit {
            putString(PREF_AUTH_TOKEN, value)
        }
    override var authTokenExpDate: Date?
        get() = sharedPreferences.getLong(PREF_AUTH_TOKEN_EXP_DATE, -1)
            .takeUnless { it == -1L }
            ?.let(::Date)
        set(value) = sharedPreferences.edit {
            if (value != null) putLong(PREF_AUTH_TOKEN_EXP_DATE, value.time)
            else remove(PREF_AUTH_TOKEN_EXP_DATE)
        }
    override var visitorId: UUID
        get() = sharedPreferences.getUUID(PREF_VISITOR_ID) ?: UUID.randomUUID().also {
            visitorId = it
        }
        set(value) = sharedPreferences.edit {
            putString(PREF_VISITOR_ID, value.toString())
        }
    override var consumerId: UUID?
        get() = sharedPreferences.getUUID(PREF_CONSUMER_ID, null)
        set(value) = sharedPreferences.edit {
            putString(PREF_CONSUMER_ID, value.toString())
        }
    override val destinationId: UUID = UUID.randomUUID()
    override var welcomeMessage: String
        get() = sharedPreferences.getStringOrEmpty(PREF_WELCOME_MESSAGE)
        set(value) = sharedPreferences.edit {
            putString(PREF_WELCOME_MESSAGE, value)
        }

    constructor(context: Context) : this(context.getSharedPreferences("$PREFIX.storage", Context.MODE_PRIVATE))

    override fun clearStorage() {
        sharedPreferences.edit()?.clear()?.apply()
    }

    private fun SharedPreferences.getStringOrEmpty(key: String, defValue: String? = null): String = getString(key, defValue).orEmpty()
    private fun SharedPreferences.getUUID(key: String, defValue: String? = null): UUID? = getString(key, defValue)?.let(UUID::fromString)

    private companion object {
        private const val PREFIX = "com.nice.cxonechat"
        private const val PREF_AUTH_TOKEN: String = "$PREFIX.share_sdk_auth_token"
        private const val PREF_AUTH_TOKEN_EXP_DATE: String = "$PREFIX.share_sdk_auth_token_exp_date"
        private const val PREF_VISITOR_ID: String = "$PREFIX.share_visitor_id"
        private const val PREF_CONSUMER_ID: String = "$PREFIX.share_consumer_id"
        private const val PREF_WELCOME_MESSAGE: String = "$PREFIX.share_welcome_message"
    }

}
