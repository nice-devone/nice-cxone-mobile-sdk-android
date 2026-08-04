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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.ChatStateEvent
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenDelegateListener
import com.nice.cxonechat.TokenRequestReason
import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.tool.ChatEntrailsMock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.WebSocket
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatImplTest {
    private lateinit var chatImpl: ChatImpl
    private lateinit var connection: Connection
    private lateinit var entrails: ChatEntrails
    private lateinit var dependencies: ChatImplDependencies
    private lateinit var configuration: ConfigurationInternal
    private lateinit var chatStateListener: ChatStateListener
    private lateinit var storage: ValueStorage
    private lateinit var retryApiHandler: RetryApiHandler

    private lateinit var authService: AuthService

    @Before
    fun setUp() {
        connection = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        entrails = ChatEntrailsMock(
            sharedClient = mockk(relaxed = true),
            storage = storage,
            service = mockk(relaxed = true),
            authService = authService,
            logger = mockk(relaxed = true),
            environment = mockk(relaxed = true),
            cookieJar = mockk(relaxed = true),
            coroutineScope = TestScope(StandardTestDispatcher()),
        )
        // Use a real ProxyWebSocketListener so that SocketConnectionListener callbacks work correctly
        // (needed since connect() now suspends at openDeferred.await() until onOpen fires).
        val proxyListener = ProxyWebSocketListener()
        dependencies = mockk(relaxed = true) {
            every { socketFactory.createProxyListener() } returns proxyListener
        }
        configuration = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        chatImpl = spyk(ChatImpl(connection, entrails, dependencies, configuration, chatStateListener))
        retryApiHandler = mockk(relaxed = true)
        chatImpl.retryApiHandler = retryApiHandler
    }

    @Test
    fun `setDeviceToken null sets storage and triggers sendVisitorInfo`() {
        every { entrails.storage.deviceToken } returns "oldToken" andThen null
        chatImpl.setDeviceToken(null)
        verify { entrails.storage.deviceToken = null }
    }

    @Test
    fun `getChannelAvailability returns isChatAvailable`() = runTest {
        chatImpl.isChatAvailable = false
        val result = chatImpl.getChannelAvailability()
        assertEquals(false, result)
    }

    @Test
    fun `createAuthRequestBody returns correct body for SecuredCookie`() {
        val body = chatImpl.createAuthRequestBody(AuthenticationType.SecuredCookie)
        assertNull(body.requestBody.type)
        assertNull(body.requestBody.customerIdentity)
        assertNull(body.requestBody.thirdParty)
    }

    @Test
    fun `createAuthRequestBody returns correct body for Anonymous`() {
        every { connection.customerId } returns "customerId"
        val body = chatImpl.createAuthRequestBody(AuthenticationType.Anonymous)
        assertEquals(AuthenticationType.Anonymous.name, body.requestBody.type)
    }

    @Test
    fun `createAuthRequestBody returns correct body for ThirdPartyOAuth explicit flow`() {
        val authorization = mockk<Authorization> {
            every { code } returns "code"
            every { verifier } returns "verifier"
        }
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = authorization,
        )
        chatImpl.dependencies = deps
        val body = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)
        assertEquals(GrantType.AUTHORIZATION_CODE, body.requestBody.thirdParty?.grantType)
        assertEquals("code", body.requestBody.thirdParty?.authorizationCode)
        assertEquals("verifier", body.requestBody.thirdParty?.codeVerifier)
    }

    @Test
    fun `createAuthRequestBody uses tokenDelegateListener for ThirdPartyOAuth implicit flow`() {
        val jwt = "integrator-jwt-token"
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        // Set delegate → implicit flow
        val tokenDelegate = TokenDelegateListener { _: TokenRequestReason -> OAuthToken(jwt) }
        val tokenDelegateField = ChatImpl::class.java.getDeclaredField("tokenDelegateListener")
        tokenDelegateField.isAccessible = true
        tokenDelegateField.set(chatImpl, tokenDelegate)

        val bodyWithMeta = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)
        assertEquals(GrantType.IMPLICIT, bodyWithMeta.requestBody.thirdParty?.grantType)
        assertEquals(jwt, bodyWithMeta.requestBody.thirdParty?.accessToken)
        assertNull(bodyWithMeta.requestBody.thirdParty?.authorizationCode)
        assertNull(bodyWithMeta.requestBody.thirdParty?.codeVerifier)
        assertEquals(jwt, bodyWithMeta.oauthToken?.accessToken)
    }

    // ── isImplicitOAuthFlow property ────────────────────────────────────────

    @Test
    fun `isImplicitOAuthFlow is true when delegate set and authorization is None`() {
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        val chat = ChatImpl(
            connection = connection,
            entrails = entrails,
            dependencies = deps,
            configuration = configuration,
            rawChatStateListener = chatStateListener,
            tokenDelegateListener = { OAuthToken("token") },
        )
        assertTrue(chat.isImplicitOAuthFlow)
    }

    @Test
    fun `isImplicitOAuthFlow is false when delegate set but authorization is not None`() {
        val authorization = Authorization("code", "verifier")
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = authorization,
        )
        val chat = ChatImpl(
            connection = connection,
            entrails = entrails,
            dependencies = deps,
            configuration = configuration,
            rawChatStateListener = chatStateListener,
            tokenDelegateListener = { OAuthToken("token") },
        )
        assertFalse(chat.isImplicitOAuthFlow)
    }

    @Test
    fun `isImplicitOAuthFlow is false when no delegate regardless of authorization`() {
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        val chat = ChatImpl(
            connection = connection,
            entrails = entrails,
            dependencies = deps,
            configuration = configuration,
            rawChatStateListener = chatStateListener,
            tokenDelegateListener = null,
        )
        assertFalse(chat.isImplicitOAuthFlow)
    }

    // ── createAuthRequestBody — Anonymous ───────────────────────────────────

    @Test
    fun `createAuthRequestBody returns null customerIdentity for Anonymous when customerId is null`() {
        every { connection.customerId } returns null
        val body = chatImpl.createAuthRequestBody(AuthenticationType.Anonymous)
        assertEquals(AuthenticationType.Anonymous.name, body.requestBody.type)
        assertNull(body.requestBody.customerIdentity)
    }

    // ── createAuthRequestBody — implicit flow reason forwarding ─────────────

    @Test
    fun `createAuthRequestBody passes UNSPECIFIED reason to delegate for implicit flow`() {
        val capturedReasons = mutableListOf<TokenRequestReason>()
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        val tokenDelegateField = ChatImpl::class.java.getDeclaredField("tokenDelegateListener")
        tokenDelegateField.isAccessible = true
        tokenDelegateField.set(chatImpl, TokenDelegateListener { reason ->
            capturedReasons += reason
            OAuthToken("test-jwt")
        })

        chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)

        assertEquals(1, capturedReasons.size)
        assertEquals(TokenRequestReason.UNSPECIFIED, capturedReasons[0])
    }

    @Test
    fun `createAuthRequestBody implicit flow body has null authorizationCode and codeVerifier`() {
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        val tokenDelegateField = ChatImpl::class.java.getDeclaredField("tokenDelegateListener")
        tokenDelegateField.isAccessible = true
        tokenDelegateField.set(chatImpl, TokenDelegateListener { OAuthToken("jwt") })

        val body = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)

        assertNull(body.requestBody.thirdParty?.authorizationCode)
        assertNull(body.requestBody.thirdParty?.codeVerifier)
        assertNull(body.requestBody.thirdParty?.refreshToken)
    }

    @Test
    fun `createAuthRequestBody explicit flow body has null accessToken and refreshToken`() {
        val authorization = mockk<Authorization> {
            every { code } returns "auth-code"
            every { verifier } returns "code-verifier"
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = authorization,
        )

        val body = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)

        assertNull(body.requestBody.thirdParty?.accessToken)
        assertNull(body.requestBody.thirdParty?.refreshToken)
        assertEquals(GrantType.AUTHORIZATION_CODE, body.requestBody.thirdParty?.grantType)
    }

    @Test
    fun `connect fetches new transaction token when no valid token`() = runTest {
        val socket = mockk<WebSocket>()
        val mockSocketFactory = mockk<SocketFactory> {
            // Use 3-arg match: Kotlin compiles create(listener, visitorId) as create(listener, visitorId, null)
            every { create(any(), any(), isNull()) } returns socket
        }
        every { storage.transactionTokenModel } returns null
        val deps = mockk<ChatImplDependencies>(relaxed = true) {
            every { socketFactory } returns mockSocketFactory
        }
        chatImpl.dependencies = deps
        val connectJob = launch {
            assertFailsWith<TransientConnectFailureException> { chatImpl.connect() }
        }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()
    }

    @Test
    fun `connect reports token delegation failure when auth body creation throws`() = runTest {
        val cause = IllegalStateException("delegate failed")
        val tokenDelegationException =
            RuntimeChatException.TokenDelegationFailedException(cause.message ?: "Token delegation failed", cause)
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth) } throws tokenDelegationException

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await()
        advanceUntilIdle()
        connectJob.join()
        verify {
            chatStateListener.onChatRuntimeException(withArg {
                assertTrue(it is RuntimeChatException.TokenDelegationFailedException)
                assertSame(cause, it.cause)
                assertEquals("delegate failed", it.message)
            })
        }
    }

    @Test
    fun `connect stores request JWT as authToken for implicit flow`() = runTest {
        val requestJwt = "google-jwt-from-delegate"
        val responseAccessToken = "response-access-token-should-not-be-stored"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )

        val tokenDelegateField = ChatImpl::class.java.getDeclaredField("tokenDelegateListener")
        tokenDelegateField.isAccessible = true
        tokenDelegateField.set(chatImpl, TokenDelegateListener { OAuthToken(requestJwt) })

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.transactionTokenModel } returns null
        every { storage.visitorId } returns UUID.randomUUID()

        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { transactionToken } returns "tx-token"
            every { isExpired } returns false
            every { customerIdentity } returns null
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns transactionTokenModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }

        val connectJob = launch { chatImpl.connect() }
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()
        verify { storage.authToken = requestJwt }
    }

    @Test
    fun `connect stores refreshed JWT when two-step implicit reconnect occurs`() = runTest {
        val staleJwt = "stale-jwt-rejected-by-backend"
        val freshJwt = "fresh-jwt-from-second-delegate-call"
        val responseAccessToken = "response-access-token-should-not-be-stored"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        val deps = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        chatImpl.dependencies = deps
        // Delegate returns stale JWT on first call (UNSPECIFIED), fresh JWT on second (TOKEN_INVALID)
        val tokenDelegateField = ChatImpl::class.java.getDeclaredField("tokenDelegateListener")
        tokenDelegateField.isAccessible = true
        tokenDelegateField.set(chatImpl, TokenDelegateListener { reason ->
            if (reason == TokenRequestReason.UNSPECIFIED) OAuthToken(staleJwt) else OAuthToken(freshJwt)
        })

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.visitorId } returns UUID.randomUUID()

        // Simulate: expired transaction token so connectWithImplicitJwt runs
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel

        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { transactionToken } returns "tx-token"
            every { isExpired } returns false
            every { customerIdentity } returns null
        }
        // Step 1: 401 (stale JWT rejected), Step 2: success (fresh JWT accepted)
        val rejectionResponse = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 401
            every { message() } returns "Unauthorized"
        }
        val successResponse = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns transactionTokenModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returnsMany listOf(
            mockk { every { execute() } returns rejectionResponse },
            mockk { every { execute() } returns successResponse },
        )

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()

        // Must store the fresh JWT, not the stale one
        verify { storage.authToken = freshJwt }
    }

    @Test
    fun `connect stores delegate JWT as authToken for implicit flow when valid cached transaction token exists`() = runTest {
        val requestJwt = "delegate-jwt-for-cached-token-path"
        val responseAccessToken = "response-access-token-should-not-be-stored"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )

        val tokenDelegateField = ChatImpl::class.java.getDeclaredField("tokenDelegateListener")
        tokenDelegateField.isAccessible = true
        tokenDelegateField.set(chatImpl, TokenDelegateListener { OAuthToken(requestJwt) })

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.visitorId } returns UUID.randomUUID()

        // Valid cached transaction token — takes Case 1: connectWithTransactionToken (no API call)
        val cachedModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { isExpired } returns false
            every { transactionToken } returns "cached-tx-token"
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { customerIdentity } returns null
        }
        every { storage.transactionTokenModel } returns cachedModel

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()

        // Delegate JWT (from UNSPECIFIED call in createAuthRequestBody) must be stored,
        // not the response transactionTokenModel.thirdParty.accessToken
        verify { storage.authToken = requestJwt }
    }

    @Test
    fun `connect stores response accessToken as authToken for explicit flow`() = runTest {
        val responseAccessToken = "explicit-response-access-token"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        val authorization = mockk<Authorization> {
            every { code } returns "auth-code"
            every { verifier } returns "verifier"
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = authorization,
        )

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.transactionTokenModel } returns null
        every { storage.visitorId } returns UUID.randomUUID()

        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { transactionToken } returns "tx-token"
            every { isExpired } returns false
            every { customerIdentity } returns null
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns transactionTokenModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()

        verify { storage.authToken = responseAccessToken }
    }

    @Test
    fun `connect throws PermanentConnectionFailureException when connector reports ConnectionTokenFailed`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { isExpired } returns true
            every { thirdParty } returns mockk {
                every { refreshToken } returns null
            }
        }
        every { storage.transactionTokenModel } returns transactionTokenModel
        // assertFailsWith inside launch consumes the exception so it doesn't propagate to the test scope.
        // PermanentConnectionFailureException extends CancellationException — assertFailsWith catches
        // it before the coroutine framework sees it, so connectJob completes normally (not cancelled).
        val connectJob = launch {
            assertFailsWith<PermanentConnectionFailureException> { chatImpl.connect() }
        }
        advanceUntilIdle()
        connectJob.join()
        assertTrue(connectJob.isCompleted)
        assertFalse(connectJob.isCancelled)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits events even when rawChatStateListener is null`() = runTest {
        val chatNoListener = spyk(ChatImpl(connection, entrails, dependencies, configuration, null))
        chatNoListener.chatStateListener.onConnecting()
        assertEquals(ChatStateEvent.Connecting, chatNoListener.stateFlow.first())
    }

    @Test
    fun `stateFlow is a SharedFlow`() {
        // Type-ascription fails to compile if the return type regresses from SharedFlow to Flow.
        @Suppress("UNUSED_VARIABLE")
        val typed: SharedFlow<ChatStateEvent> = chatImpl.stateFlow
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow replays last event to new collector`() = runTest {
        chatImpl.chatStateListener.onReady()
        // New collector subscribes AFTER emission — should get replay
        val received = chatImpl.stateFlow.first()
        assertEquals(ChatStateEvent.Ready, received)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits Connected when onConnected is called`() = runTest {
        chatImpl.chatStateListener.onConnected()
        assertEquals(ChatStateEvent.Connected, chatImpl.stateFlow.first())
        verify { chatStateListener.onConnected() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits UnexpectedDisconnect when onUnexpectedDisconnect is called`() = runTest {
        chatImpl.chatStateListener.onUnexpectedDisconnect()
        assertEquals(ChatStateEvent.UnexpectedDisconnect, chatImpl.stateFlow.first())
        verify { chatStateListener.onUnexpectedDisconnect() }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `stateFlow emits RuntimeException wrapping the original exception`() = runTest {
        val cause = mockk<RuntimeChatException>()
        chatImpl.chatStateListener.onChatRuntimeException(cause)
        val event = chatImpl.stateFlow.first()
        assertTrue(event is ChatStateEvent.RuntimeException)
        assertSame(cause, event.exception)
        verify { chatStateListener.onChatRuntimeException(cause) }
    }

    /**
     * close() is now `runBlocking { closeSuspending() }`, which genuinely blocks the calling
     * thread until the storage/cookie drain completes. Testing "close() waits for the drain"
     * therefore requires running close() on a separate thread while the test thread controls the
     * drain gate -- calling close() on the test thread itself would deadlock (the gate can never
     * be released). A CountDownLatch proves close() has actually reached the gated await, not
     * merely that it hasn't returned yet; all waits are bounded so a regression here fails fast
     * instead of hanging. This pattern is shared by the four tests below.
     */
    @Test
    fun `close drains pending storage writes before finishing`() {
        val writeCompleted = AtomicBoolean(false)
        val gate = CompletableDeferred<Unit>()
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }
        entrails.threading.storageWriteScope.launch {
            gate.await()
            writeCompleted.set(true)
        }
        gate.complete(Unit)

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }
            .apply { isDaemon = true; start() }
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")
        assertTrue(writeCompleted.get(), "close() must drain pending writes before returning")
    }

    @Test
    fun `close drains pending cookie writes before finishing`() {
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }
            .apply { isDaemon = true; start() }
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")
        coVerify { entrails.cookieJar.awaitPendingWrites() }
    }

    @Test
    fun `close leaves storage write scope usable for a later reconnect`() {
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }
            .apply { isDaemon = true; start() }
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")

        // close() keeps connect()/events() usable per the public contract — storage writes issued
        // by the next session on this instance must still execute.
        val lateWriteCompleted = AtomicBoolean(false)
        val lateWrite = entrails.threading.storageWriteScope.launch { lateWriteCompleted.set(true) }
        runBlocking { lateWrite.join() }
        assertTrue(
            lateWriteCompleted.get(),
            "storage write after close() must execute — close() must not cancel the storage scope"
        )
    }

    @Test
    fun `drain sentinel does not cancel jobs launched after close`() {
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }
            .apply { isDaemon = true; start() }
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        // A later connect() on this instance launches fresh listener jobs on the chat scope while
        // close() is still draining — those jobs must not be swept by close()'s own cleanup.
        val freshConnectJob = entrails.threading.coroutineScope.launch { awaitCancellation() }

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")
        assertTrue(
            freshConnectJob.isActive,
            "close() must not cancel jobs launched by a connect() that follows it"
        )
        freshConnectJob.cancel()
    }

    @Test
    fun `closeSuspending does not deadlock when invoked from within storageWriteScope`() {
        val completed = AtomicBoolean(false)

        // closeSuspending()'s own coroutine would be a child of storageJob here -- without
        // excluding itself, storageJob.children.joinAll() would wait on its own (currently
        // running, not-yet-complete) Job forever.
        val job = entrails.threading.storageWriteScope.launch {
            chatImpl.closeSuspending()
            completed.set(true)
        }

        runBlocking { withTimeout(5_000) { job.join() } }
        assertTrue(completed.get(), "closeSuspending() invoked from storageWriteScope must not deadlock on its own Job")
    }

    /**
     * Regression test: signOut() must cancel in-flight session work (e.g. a token refresh
     * already past its network await, like TokenRefreshHelper.refreshAccessToken()) before
     * clearing storage -- otherwise that coroutine can still reach a property setter and
     * persist fresh credentials via storageWriteScope after clearStorage() already ran,
     * re-persisting them after logout.
     */
    @Test
    fun `signOut cancels in-flight session work before clearing storage`() {
        lateinit var sessionJob: Job
        // Job.cancel() flips isCancelled synchronously regardless of dispatcher/thread, so
        // checking it inside the clearStorage() mock answer directly proves ordering -- no
        // dependency on when the cancelled coroutine's own cleanup happens to run.
        var sessionCancelledBeforeClear = false
        coEvery { storage.clearStorage() } coAnswers {
            sessionCancelledBeforeClear = sessionJob.isCancelled
        }

        sessionJob = entrails.threading.coroutineScope.launch { awaitCancellation() }
        val testScheduler = (entrails.threading.coroutineScope as TestScope).testScheduler
        testScheduler.advanceUntilIdle()
        assertTrue(sessionJob.isActive, "session job must be active (suspended) before signOut() runs")

        // closeSuspending() now joins the cancelled session job before proceeding, which needs this
        // virtual-time scheduler pumped -- run signOut() on its own thread and pump concurrently.
        var signOutError: Throwable? = null
        val signOutThread = Thread {
            runCatching { runBlocking { withTimeout(5_000) { chatImpl.signOut() } } }.onFailure { signOutError = it }
        }.apply { isDaemon = true; start() }
        val deadline = System.nanoTime() + SECONDS.toNanos(5)
        while (signOutThread.isAlive && System.nanoTime() < deadline) {
            testScheduler.advanceUntilIdle()
        }
        signOutThread.join(1_000)
        assertFalse(signOutThread.isAlive, "signOut() must complete")
        assertNull(signOutError, "signOut() must not throw")

        assertTrue(sessionCancelledBeforeClear, "signOut() must cancel in-flight session work before calling clearStorage()")
    }

    /**
     * Regression test: cancellation only lands at a suspend point. A session job blocked past its
     * last one can still launch a storage write before it notices. closeSuspending() must join the
     * cancelled children before draining, so that write is already captured -- otherwise it races
     * past close()/signOut() and can persist stale data after they've already returned.
     *
     * Uses real dispatchers (not the shared virtual-time TestScope) so the race can be staged
     * deterministically with latches instead of a scheduler.
     */
    @Test
    fun `closeSuspending awaits a session job's cancellation cleanup before draining storage writes`() {
        val realScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val realStorageDispatcher = Dispatchers.IO.limitedParallelism(1)
        val realThreading = ThreadingExecutor(
            coroutineScope = realScope,
            storageWriteScope = CoroutineScope(
                realScope.coroutineContext + SupervisorJob(realScope.coroutineContext[Job]) + realStorageDispatcher
            ),
            storageDispatcher = realStorageDispatcher,
        )
        val realEntrails = object : ChatEntrails by entrails { override val threading = realThreading }
        val chat = ChatImpl(connection, realEntrails, dependencies, configuration, chatStateListener)

        val readyToRace = CountDownLatch(1)
        val proceed = CountDownLatch(1)
        val writeGate = CompletableDeferred<Unit>()
        val lateWriteCompleted = AtomicBoolean(false)
        realScope.launch {
            readyToRace.countDown()
            proceed.await() // blocks the thread, not a coroutine suspend point -- cancel() won't stop this
            realThreading.storageWriteScope.launch {
                writeGate.await()
                lateWriteCompleted.set(true)
            }
        }
        assertTrue(readyToRace.await(5, SECONDS), "session job must be running before close() starts")

        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers { awaitPendingWritesEntered.countDown() }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chat.close() }.onFailure { closeError = it } }
            .apply { isDaemon = true; start() }
        assertFalse(
            awaitPendingWritesEntered.await(300, TimeUnit.MILLISECONDS),
            "close() must not reach awaitPendingWrites() before the cancelled session job's cleanup completes"
        )

        proceed.countDown()
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must proceed once cleanup completes")
        assertTrue(closeThread.isAlive, "close() must still be draining the late write")

        writeGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the late write completes")
        assertNull(closeError, "close() must not throw")
        assertTrue(lateWriteCompleted.get(), "close() must await the write launched during session-job cancellation cleanup")
    }

    @Test
    fun `connect throws TransientConnectFailureException when connector reports non-ConnectionTokenFailed error`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.SecuredCookie
        every { storage.transactionTokenModel } returns null  // forces Case 3: fetch new token
        val failingTokenCall = mockk<Call<TransactionTokenModel>> {
            every { execute() } returns Response.error(503, mockk(relaxed = true))
        }
        every { entrails.authService.getTransactionToken(any(), any(), any(), any()) } returns failingTokenCall
        val connectJob = launch {
            assertFailsWith<TransientConnectFailureException> { chatImpl.connect() }
        }
        advanceUntilIdle()
        connectJob.join()
        assertTrue(connectJob.isCompleted)
        assertFalse(connectJob.isCancelled)
    }

    @Test
    fun `connect throws TransientConnectFailureException when socket onFailure fires while awaiting open`() = runTest {
        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), isNull()) } returns socket
        }
        val deps = mockk<ChatImplDependencies>(relaxed = true) {
            every { this@mockk.socketFactory } returns socketFactory
        }
        chatImpl.dependencies = deps
        // assertFailsWith inside launch consumes the exception so it doesn't propagate to the test scope.
        // TransientConnectFailureException (extends Exception, not CancellationException) would otherwise fail the test.
        val connectJob = launch {
            assertFailsWith<TransientConnectFailureException> { chatImpl.connect() }
        }
        advanceUntilIdle()
        // Simulate socket failure; connect() should throw TransientConnectFailureException (retriable by ReconnectingListener)
        chatImpl.socketListener.onFailure(socket, RuntimeException("Connection refused"), null)
        connectJob.join()
        assertTrue(connectJob.isCompleted)
        assertFalse(connectJob.isCancelled)
    }
}
