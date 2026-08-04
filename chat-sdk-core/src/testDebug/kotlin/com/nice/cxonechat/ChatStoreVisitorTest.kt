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

package com.nice.cxonechat

import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestUUIDValue
import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.internal.ChatStoreVisitor
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.ThreadingExecutor
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.tool.nextString
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID
import kotlin.random.Random

internal class ChatStoreVisitorTest {

    @Test
    fun sendVisitorInfo_doesNotRetryOnClientError() = runTest {
        val callback = mockk<Callback<Void>>(relaxed = true)
        val callbacks = slot<Callback<Void>>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mockStorage = mockk<ValueStorage> {
            every { visitorId } returns TestUUIDValue
            every { deviceToken } returns null
        }
        val testExecutor = ThreadingExecutor(
            mainDispatcher = dispatcher,
            backgroundDispatcher = dispatcher,
            ioDispatcher = dispatcher,
            storageDispatcher = dispatcher,
            coroutineScope = this,
            storageWriteScope = this,
        )

        val mockCall = mockk<Call<Void>> {
            every { enqueue(capture(callbacks)) } answers {
                callbacks.captured.onResponse(this@mockk, Response.error(400, "".toResponseBody(null)))
            }
            every { cancel() } returns Unit
        }
        val mockService = mockk<RemoteService> {
            every { createOrUpdateVisitor(any(), any(), any()) } returns mockCall
        }
        val mockChat = mockk<ChatWithParameters>(relaxed = false) {
            every { service } returns mockService
            every { storage } returns mockStorage
            every { entrails } returns mockk()
            every { entrails.service } returns mockService
            every { entrails.storage } returns mockStorage
            every { entrails.threading } returns testExecutor
            every { entrails.logger } returns mockk(relaxed = true)
            every { chatStateListener } returns null
            every { connection } returns mockk {
                every { brandId } returns Random.nextInt()
                every { visitorId } returns UUID.randomUUID()
                every { customerId } returns "TEST"
                every { firstName } returns nextString()
                every { lastName } returns nextString()
            }
        }

        ChatStoreVisitor(mockChat, callback)

        verify(exactly = 0) { callback.onResponse(any(), any()) }
    }

    /**
     * Plain (non-runTest) test, matching ChatDecoratorCloseCascadeTest -- close() bridges via
     * runBlocking, which must never be called from within a runTest coroutine. The constructor's
     * fire-and-forget sendVisitorInfo() init block needs a genuinely working CoroutineScope
     * (Dispatchers.Unconfined runs it eagerly, synchronously) rather than a bare relaxed mock.
     */
    @Test
    fun `close cascades to origin closeSuspending, never origin close`() {
        val realScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val testExecutor = ThreadingExecutor(
            mainDispatcher = Dispatchers.Unconfined,
            backgroundDispatcher = Dispatchers.Unconfined,
            ioDispatcher = Dispatchers.Unconfined,
            storageDispatcher = Dispatchers.Unconfined,
            coroutineScope = realScope,
            storageWriteScope = realScope,
        )
        val mockCall = mockk<Call<Void>>(relaxed = true)
        val mockService = mockk<RemoteService> {
            every { createOrUpdateVisitor(any(), any(), any()) } returns mockCall
        }
        val mockStorage = mockk<ValueStorage>(relaxed = true) {
            every { visitorId } returns TestUUIDValue
        }
        val mockChat = mockk<ChatWithParameters>(relaxed = true) {
            every { entrails } returns mockk(relaxed = true) {
                every { service } returns mockService
                every { storage } returns mockStorage
                every { threading } returns testExecutor
                every { logger } returns mockk(relaxed = true)
            }
            every { connection } returns mockk(relaxed = true) {
                every { brandId } returns Random.nextInt()
                every { visitorId } returns UUID.randomUUID()
                every { customerId } returns "TEST"
                every { firstName } returns nextString()
                every { lastName } returns nextString()
            }
        }

        val visitor = ChatStoreVisitor(mockChat, mockk(relaxed = true))
        visitor.close()

        coVerify(exactly = 1) { mockChat.closeSuspending() }
        verify(exactly = 0) { mockChat.close() }
    }
}
