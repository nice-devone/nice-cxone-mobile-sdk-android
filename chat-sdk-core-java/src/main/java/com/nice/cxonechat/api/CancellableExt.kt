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

package com.nice.cxonechat.api

import androidx.annotation.CheckResult
import com.nice.cxonechat.Cancellable
import kotlinx.coroutines.Job

/**
 * Adapts a coroutine [Job] to the SDK's Java-facing [Cancellable] handle.
 *
 * The returned [Cancellable] holds a `::cancel` method reference on the receiver [Job] — invoking
 * [Cancellable.cancel] forwards to [Job.cancel] (with no `cause`), which is idempotent: calling it
 * after the job has already completed or been cancelled is a no-op. The receiver [Job] stays
 * referenced for the lifetime of the [Cancellable] so the caller can release the operation at any
 * later point.
 *
 * The method reference deliberately does NOT capture or close over any [kotlinx.coroutines.CoroutineScope]
 * — that matches the contract of every `chat-sdk-core-java` interop helper, which guarantees the
 * launching scope is wrapper-owned (cancelled in `AutoCloseable.close`) or one-shot (cancelled in
 * `invokeOnCompletion`). Consumers see only the [Cancellable] handle.
 *
 * Annotated [CheckResult] because dropping the returned [Cancellable] without invoking [cancel]
 * leaks the [Job] (and, for observer helpers, the upstream Flow subscription) until the scope
 * finally tears down — which for hot SharedFlow-backed observers is the process lifetime.
 *
 * `@JvmSynthetic` keeps it invisible to Java consumers; the only Java exit point for the
 * interop helpers is the public [Cancellable] return value.
 */
@CheckResult
@JvmSynthetic
internal fun Job.asCancellable() = Cancellable(::cancel)
