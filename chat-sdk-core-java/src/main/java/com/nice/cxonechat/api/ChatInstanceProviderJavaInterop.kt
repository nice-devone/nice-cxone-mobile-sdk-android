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

@file:JvmName("ChatInstanceProviderJavaInterop")

package com.nice.cxonechat.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatInstanceProvider.ConfigurationScope
import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Java-friendly asynchronous variant of [ChatInstanceProvider.closeSuspending].
 *
 * [ChatInstanceProvider.close] is already directly callable from Java (it blocks the calling
 * thread); this variant is for callers who want the same cleanup without blocking a thread.
 *
 * See [connectAsync] for notes on null `onError` and exceptions thrown by `onSuccess`.
 *
 * @param provider The [ChatInstanceProvider] instance to close.
 * @param onSuccess Invoked when the close completes successfully. May be null.
 * @param onError Invoked when an error occurs. May be null.
 * @param callbackExecutor If non-null, used as the launched coroutine's dispatcher so callbacks
 *                         run on the executor's thread. When null, callbacks fire on the SDK's
 *                         internal IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
fun closeSuspendingAsync(
    provider: ChatInstanceProvider,
    onSuccess: Runnable? = null,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { closeProviderSuspending(provider, onError, onSuccess) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun closeProviderSuspending(
    provider: ChatInstanceProvider,
    onError: ErrorCallback? = null,
    onSuccess: Runnable? = null,
) {
    try {
        provider.closeSuspending()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e)
        return
    } catch (expected: Exception) {
        onError?.onError(InternalError("Unexpected error closing ChatInstanceProvider", expected))
        return
    }
    // Outside the SDK try/catch: consumer bugs in onSuccess do NOT get reported as SDK errors.
    onSuccess?.run()
}

/**
 * Java-friendly blocking variant of [ChatInstanceProvider.signOut].
 *
 * Kotlin consumers should prefer the suspend [ChatInstanceProvider.signOut] directly.
 *
 * **Must NOT be called on the main thread** -- performs blocking storage/cookie persistence and
 * socket teardown.
 *
 * @param provider The [ChatInstanceProvider] instance to sign out.
 */
@Public
@WorkerThread
fun signOutBlocking(provider: ChatInstanceProvider): Unit = runBlocking { provider.signOut() }

/**
 * Java-friendly asynchronous variant of [ChatInstanceProvider.signOut].
 *
 * Internally launches a background coroutine and notifies the supplied callbacks. Returns a
 * [Cancellable] that cancels the underlying work.
 *
 * See [connectAsync] for notes on null `onError` and exceptions thrown by `onSuccess`.
 *
 * @param provider The [ChatInstanceProvider] instance to sign out.
 * @param onSuccess Invoked when sign-out completes successfully. May be null.
 * @param onError Invoked when an error occurs. May be null.
 * @param callbackExecutor If non-null, used as the launched coroutine's dispatcher so callbacks
 *                         run on the executor's thread. When null, callbacks fire on the SDK's
 *                         internal IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
fun signOutAsync(
    provider: ChatInstanceProvider,
    onSuccess: Runnable? = null,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { signOutProvider(provider, onError, onSuccess) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun signOutProvider(
    provider: ChatInstanceProvider,
    onError: ErrorCallback? = null,
    onSuccess: Runnable? = null,
) {
    try {
        provider.signOut()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e)
        return
    } catch (expected: Exception) {
        onError?.onError(InternalError("Unexpected error signing out ChatInstanceProvider", expected))
        return
    }
    onSuccess?.run()
}

/**
 * Java-friendly blocking variant of [ChatInstanceProvider.configure].
 *
 * Kotlin consumers should prefer the suspend [ChatInstanceProvider.configure] directly.
 *
 * **Must NOT be called on the main thread** -- performs blocking work while stopping and
 * restarting the chat session.
 *
 * @param provider The [ChatInstanceProvider] instance to reconfigure.
 * @param context [Context] for resource access.
 * @param actions Invoked with the [ConfigurationScope] to apply configuration changes.
 */
@Public
@WorkerThread
fun configureBlocking(
    provider: ChatInstanceProvider,
    context: Context,
    actions: Consumer<ConfigurationScope>,
): Unit = runBlocking { provider.configure(context) { actions.accept(this) } }

/**
 * Java-friendly asynchronous variant of [ChatInstanceProvider.configure].
 *
 * Internally launches a background coroutine and notifies the supplied callbacks. Returns a
 * [Cancellable] that cancels the underlying work.
 *
 * See [connectAsync] for notes on null `onError` and exceptions thrown by `onSuccess`.
 *
 * @param provider The [ChatInstanceProvider] instance to reconfigure.
 * @param context [Context] for resource access.
 * @param actions Invoked with the [ConfigurationScope] to apply configuration changes.
 * @param onSuccess Invoked when reconfiguration completes successfully. May be null.
 * @param onError Invoked when an error occurs. May be null.
 * @param callbackExecutor If non-null, used as the launched coroutine's dispatcher so callbacks
 *                         run on the executor's thread. When null, callbacks fire on the SDK's
 *                         internal IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
@Suppress("LongParameterList") // Java-interop signature needs every parameter for full control.
fun configureAsync(
    provider: ChatInstanceProvider,
    context: Context,
    actions: Consumer<ConfigurationScope>,
    onSuccess: Runnable? = null,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) {
    configureProvider(provider, context, actions, onError, onSuccess)
}

@VisibleForTesting
@JvmSynthetic
internal suspend fun configureProvider(
    provider: ChatInstanceProvider,
    context: Context,
    actions: Consumer<ConfigurationScope>,
    onError: ErrorCallback? = null,
    onSuccess: Runnable? = null,
) {
    // Tracks whether the failure happened inside the consumer's own `actions` lambda -- as opposed
    // to configure()'s own signOut()/prepare() steps -- so it can propagate unwrapped below, matching
    // connectChat()'s convention that consumer callback failures are not reported as SDK errors.
    var runningConsumerAction = false
    try {
        provider.configure(context) {
            runningConsumerAction = true
            actions.accept(this)
            runningConsumerAction = false
        }
    } catch (e: CancellationException) {
        throw e
    } catch (expected: Exception) {
        if (runningConsumerAction) throw expected
        val error = expected as? CXoneException
            ?: InternalError("Unexpected error configuring ChatInstanceProvider", expected)
        onError?.onError(error)
        return
    }
    onSuccess?.run()
}
