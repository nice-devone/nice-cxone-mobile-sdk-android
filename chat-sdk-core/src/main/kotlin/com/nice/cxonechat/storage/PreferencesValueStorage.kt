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
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.util.UUID
import kotlin.time.Instant
import kotlin.time.Instant.Companion.fromEpochMilliseconds

@Suppress("LongParameterList")
internal class PreferencesValueStorage private constructor(
    private val store: EncryptedDataStoreContract,
    private val scope: CoroutineScope,
    private val logger: Logger,
    initialAuthToken: String?,
    initialAuthTokenExpDate: Instant?,
    initialTransactionTokenModel: TransactionTokenModel?,
    initialCustomerId: String?,
    initialVisitorId: UUID,
    initialVisitDetails: VisitDetails?,
    initialWelcomeMessage: String,
    initialDeviceToken: String?,
) : ValueStorage {

    private val _authToken = MutableStateFlow(initialAuthToken)
    override var authToken: String?
        get() = _authToken.value
        set(value) {
            _authToken.value = value
            // Re-read the state inside the coroutine so that a clearStorage() call that
            // fires between the state-write and the enqueue is observable: if clearStorage()
            // raced ahead and nulled the field, the coroutine writes the null sentinel rather
            // than the stale value — which store.clear() then wipes anyway.
            scope.launch(CoroutineName("write:authToken")) { store.putString(PREF_AUTH_TOKEN, _authToken.value) }
        }

    private val _authTokenExpDate = MutableStateFlow(initialAuthTokenExpDate)
    override var authTokenExpDate: Instant?
        get() = _authTokenExpDate.value
        set(value) {
            _authTokenExpDate.value = value
            scope.launch(CoroutineName("write:authTokenExpDate")) {
                val current = _authTokenExpDate.value
                if (current != null) {
                    store.putString(PREF_AUTH_TOKEN_EXP_DATE, current.toEpochMilliseconds().toString())
                } else {
                    store.remove(PREF_AUTH_TOKEN_EXP_DATE)
                }
            }
        }

    private val _transactionTokenModel = MutableStateFlow(initialTransactionTokenModel)
    override var transactionTokenModel: TransactionTokenModel?
        get() = _transactionTokenModel.value
        set(value) {
            _transactionTokenModel.value = value
            scope.launch(CoroutineName("write:transactionTokenModel")) {
                val current = _transactionTokenModel.value
                if (current != null) {
                    store.putString(PREF_TRANSACTION_TOKEN, Default.serializer.encodeToString(current))
                } else {
                    store.remove(PREF_TRANSACTION_TOKEN)
                }
            }
        }

    private val _customerId = MutableStateFlow(initialCustomerId)
    override var customerId: String?
        get() = _customerId.value
        set(value) {
            _customerId.value = value
            scope.launch(CoroutineName("write:customerId")) {
                _customerId.value?.let { store.putString(PREF_CUSTOMER_ID, it) } ?: store.remove(PREF_CUSTOMER_ID)
            }
        }

    private val _visitorId = MutableStateFlow(initialVisitorId)
    override val visitorId: UUID get() = _visitorId.value

    private val _visitDetails = MutableStateFlow(initialVisitDetails)
    override var visitDetails: VisitDetails?
        get() = _visitDetails.value
        set(value) {
            _visitDetails.value = value
            scope.launch(CoroutineName("write:visitDetails")) {
                val current = _visitDetails.value
                if (current != null) {
                    store.putString(PREF_VISIT_DETAILS, Default.serializer.encodeToString(current))
                } else {
                    store.remove(PREF_VISIT_DETAILS)
                }
            }
        }

    override val visitId: UUID
        get() {
            val existing = _visitDetails.value
            if (existing != null) return existing.visitId
            val created = VisitDetails()
            // CAS: only the winner persists and returns their UUID; losers read below.
            // MutableStateFlow.compareAndSet matches the expected slot structurally; for the
            // expected null this is identical to the previous AtomicReference identity check.
            return if (_visitDetails.compareAndSet(null, created)) {
                scope.launch(CoroutineName("write:visitDetails:init")) {
                    // Re-read inside the coroutine (mirrors setter pattern): if clearStorage() raced
                    // ahead and nulled the reference, skip the write — the clear job handles cleanup.
                    val current = _visitDetails.value
                    if (current != null) {
                        store.putString(PREF_VISIT_DETAILS, Default.serializer.encodeToString(current))
                    }
                }
                created.visitId
            } else {
                // A concurrent clearStorage() or visitDetails = null can null the reference between
                // a failed CAS and this read; the fallback UUID is ephemeral and never persisted —
                // the next visitId access may re-enter this path and generate a stable one.
                _visitDetails.value?.visitId ?: UUIDProvider.next()
            }
        }

    override val visitValidUntil: Instant?
        get() = _visitDetails.value?.validUntil

    override val destinationId: UUID = UUIDProvider.next()

    private val _welcomeMessage = MutableStateFlow(initialWelcomeMessage)
    override var welcomeMessage: String
        get() = _welcomeMessage.value
        set(value) {
            _welcomeMessage.value = value
            scope.launch(CoroutineName("write:welcomeMessage")) { store.putString(PREF_WELCOME_MESSAGE, _welcomeMessage.value) }
        }

    private val _deviceToken = MutableStateFlow(initialDeviceToken)
    override var deviceToken: String?
        get() = _deviceToken.value
        set(value) {
            _deviceToken.value = value
            scope.launch(CoroutineName("write:deviceToken")) { store.putString(PREF_DEVICE_TOKEN, _deviceToken.value) }
        }

    override suspend fun clearStorage() {
        // Reset all backing fields directly (not via setters) to avoid enqueuing wasted per-key
        // writes that would be immediately clobbered by store.clear(). Using setters would also
        // produce redundant null-sentinel writes: each setter re-reads the already-reset state
        // inside its launched block and writes null, which store.clear() then discards anyway.
        _authToken.value = null
        _authTokenExpDate.value = null
        _transactionTokenModel.value = null
        _customerId.value = null
        _visitDetails.value = null
        _welcomeMessage.value = ""
        _deviceToken.value = null

        // Drain any sibling-field write already launched by a property setter before this call --
        // otherwise, if that write's own dataStore.edit() is still in flight when store.clear()
        // runs below, DataStore's internal write-actor (not this code) decides whether the clear
        // or the stale write lands last, and the stale write can resurrect data store.clear() just
        // wiped (e.g. a token-refresh write racing signOut(), leaving an old auth token persisted
        // after logout). Snapshotting here (before any new write from below can be launched) and
        // excluding self (mirrors PersistentCookieJar.awaitPendingWrites()) guards against both an
        // unrelated in-flight write and a self-join if this is ever invoked from within scope.
        val thisJob = currentCoroutineContext()[Job]
        scope.coroutineContext[Job]?.children
            ?.filter { it !== thisJob }
            ?.toList()
            ?.joinAll()

        // This suspend fun does not itself serialize against a concurrent, unawaited create() --
        // no such caller exists. Chat.signOut() awaits clearStorage() fully before returning, and
        // ChatInstanceProvider.signOut() awaits that before advancing state, so a rebuild's
        // create() on the same store can only start after this has already completed; ordering
        // comes from that caller-side await chain, not from any dispatcher or lock in this class.
        // (limitedParallelism(1) on the write dispatcher does NOT provide this on its own -- it
        // only bounds concurrent execution, and does not hold across a suspension point such as
        // DataStore's edit(), so it cannot be relied on for cross-call ordering.)
        // Failures are logged rather than propagated, matching every other field's fire-and-forget
        // write: signOut()'s cookie cleanup/close() must still run.
        val freshVisitorId = UUIDProvider.next()
        _visitorId.value = freshVisitorId
        try {
            store.clear()
            store.putString(PREF_VISITOR_ID, freshVisitorId.toString())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (expected: Exception) {
            logger.warning("Failed to persist cleared storage", expected)
        }
    }

    companion object {
        private const val PREF_AUTH_TOKEN: String = "share_sdk_auth_token"
        private const val PREF_TRANSACTION_TOKEN: String = "share_sdk_refresh_token"
        private const val PREF_AUTH_TOKEN_EXP_DATE: String = "share_sdk_auth_token_exp_date"
        private const val PREF_VISITOR_ID: String = "share_visitor_id"
        private const val PREF_CUSTOMER_ID: String = "share_customer_id"
        private const val PREF_VISIT_DETAILS: String = "share_visit_details"
        private const val PREF_WELCOME_MESSAGE: String = "share_welcome_message"
        private const val PREF_DEVICE_TOKEN: String = "device_token"

        internal suspend fun create(
            store: EncryptedDataStoreContract,
            scope: CoroutineScope,
            logger: Logger = LoggerNoop,
        ): PreferencesValueStorage {
            val loggerScope = LoggerScope("PreferencesValueStorage/create", logger)
            val visitorId = store.readOrGenerateVisitorId(loggerScope)
            return PreferencesValueStorage(
                store = store,
                scope = scope,
                logger = logger,
                initialAuthToken = store.getString(PREF_AUTH_TOKEN).firstOrNull(),
                initialAuthTokenExpDate = store.getString(PREF_AUTH_TOKEN_EXP_DATE).firstOrNull()
                    ?.toLongOrNull()
                    ?.let(::fromEpochMilliseconds),
                initialTransactionTokenModel = store.getString(PREF_TRANSACTION_TOKEN).firstOrNull()
                    ?.let { json ->
                        try {
                            Default.serializer.decodeFromString<TransactionTokenModel>(json)
                        } catch (ignored: SerializationException) {
                            loggerScope.warning(
                                "Discarding malformed $PREF_TRANSACTION_TOKEN from store; SDK will require re-authentication",
                            )
                            store.tryRemoveCorruptKey(PREF_TRANSACTION_TOKEN, loggerScope)
                            null
                        }
                    },
                initialCustomerId = store.getString(PREF_CUSTOMER_ID).firstOrNull(),
                initialVisitorId = visitorId,
                initialVisitDetails = store.getString(PREF_VISIT_DETAILS).firstOrNull()
                    ?.let { json ->
                        try {
                            Default.serializer.decodeFromString<VisitDetails>(json)
                        } catch (ignored: SerializationException) {
                            loggerScope.warning("Discarding malformed $PREF_VISIT_DETAILS from store; visit session will be reset")
                            store.tryRemoveCorruptKey(PREF_VISIT_DETAILS, loggerScope)
                            null
                        }
                    },
                initialWelcomeMessage = store.getString(PREF_WELCOME_MESSAGE).firstOrNull().orEmpty(),
                initialDeviceToken = store.getString(PREF_DEVICE_TOKEN).firstOrNull(),
            )
        }

        suspend fun create(
            context: Context,
            scope: CoroutineScope,
            logger: Logger = LoggerNoop,
        ): PreferencesValueStorage {
            val store = EncryptedDataStoreProvider.getInstance(context, logger)
            return create(store, scope, logger)
        }

        private suspend fun EncryptedDataStoreContract.getStoreVisitorId(): String? =
            getString(PREF_VISITOR_ID).firstOrNull()

        private suspend fun EncryptedDataStoreContract.putStoreVisitorId(visitorId: UUID, logger: Logger) {
            try {
                putString(PREF_VISITOR_ID, visitorId.toString())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (expected: Exception) {
                logger.warning("Failed to persist $PREF_VISITOR_ID", expected)
            }
        }

        private suspend fun EncryptedDataStoreContract.readOrGenerateVisitorId(logger: Logger): UUID {
            val stored = getStoreVisitorId() ?: return generateAndPersistVisitorId(logger)
            return try {
                UUID.fromString(stored)
            } catch (ignored: IllegalArgumentException) {
                logger.warning("Stored $PREF_VISITOR_ID '$stored' is not a valid UUID; generating new visitor ID")
                generateAndPersistVisitorId(logger)
            }
        }

        private suspend fun EncryptedDataStoreContract.generateAndPersistVisitorId(logger: Logger): UUID =
            // putStoreVisitorId() already catches and logs persistence failures -- no need to
            // duplicate that handling here.
            UUIDProvider.next().also { fresh -> putStoreVisitorId(fresh, logger) }

        private suspend fun EncryptedDataStoreContract.tryRemoveCorruptKey(key: String, logger: Logger) {
            try {
                remove(key)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (expected: Exception) {
                logger.warning("Failed to remove corrupt $key from store", expected)
            }
        }
    }
}
