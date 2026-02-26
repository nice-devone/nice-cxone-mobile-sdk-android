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

package com.nice.cxonechat.logger

import com.nice.cxonechat.log.Level
import com.nice.cxonechat.logger.fake.CollectingLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress(
    "LargeClass", // Large test class is acceptable for comprehensive unit testing of RemoteLogger functionality
)
internal class RemoteLoggerTest {

    private lateinit var errorLogger: CollectingLogger
    private lateinit var mockClient: OkHttpClient
    private lateinit var mockCall: Call
    private lateinit var remoteLogger: RemoteLogger
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        errorLogger = CollectingLogger()
        mockCall = mockk(relaxed = true)
        mockClient = mockk(relaxed = true)

        // Mock the companion object to bypass Android API dependencies
        mockkObject(RemoteLogger.Companion)
        every { RemoteLogger.setData(any(), any(), any(), any()) } answers {
            // Get access to the companion's private fields using reflection
            val finalLoggerUrlField = RemoteLogger::class.java.getDeclaredField(FINAL_LOGGER_URL_FIELD).apply {
                isAccessible = true
            }
            finalLoggerUrlField.set(null, TEST_LOGGER_URL)

            val deviceFingerprintField = RemoteLogger::class.java.getDeclaredField(DEVICE_FINGERPRINT_FIELD).apply {
                isAccessible = true
            }
            deviceFingerprintField.set(null, TEST_FINGERPRINT)
        }

        // Call setData (which is now mocked)
        RemoteLogger.setData(
            brandId = 123L,
            loggerUrl = TEST_BASE_LOGGER_URL,
            chatUrl = "https://channels.test.example.com/chat/",
            deviceFingerprint = TEST_FINGERPRINT
        )

        every { mockClient.newCall(any()) } returns mockCall

        remoteLogger = RemoteLogger(
            version = TEST_VERSION,
            okHttpClient = mockClient,
            errorLogger = errorLogger,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `errors are logged to errorLogger when HTTP request fails with non-2xx status code`() = runTest(testDispatcher) {
        // Arrange
        val requestSlot = slot<Request>()
        every { mockClient.newCall(capture(requestSlot)) } returns mockCall

        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Internal Server Error")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act
        remoteLogger.log(Level.Error, "Test error message", null)

        // Allow coroutine to complete
        advanceUntilIdle()

        // Assert
        assertTrue(errorLogger.logged.isNotEmpty(), ERROR_LOGGED_MESSAGE)
        val loggedError = errorLogger.logged.first()
        assertEquals(Level.Error, loggedError.level)
        assertTrue(
            loggedError.message.contains(REMOTE_LOGGER_PREFIX),
            "Expected message to contain '$REMOTE_LOGGER_PREFIX' prefix"
        )
        assertTrue(
            loggedError.message.contains(FAILED_TO_LOG_MESSAGE),
            "Expected message to contain error description"
        )
        assertTrue(
            loggedError.message.contains("500"),
            "Expected message to contain status code"
        )
    }

    @Test
    fun `errors are logged to errorLogger when IOException occurs`() = runTest(testDispatcher) {
        // Arrange
        val testException = IOException("Network connection failed")
        every { mockCall.execute() } throws testException

        // Act
        remoteLogger.log(Level.Error, "Test error message", null)

        // Allow coroutine to complete
        advanceUntilIdle()

        // Assert
        assertTrue(errorLogger.logged.isNotEmpty(), ERROR_LOGGED_MESSAGE)
        val loggedError = errorLogger.logged.first()
        assertEquals(Level.Error, loggedError.level)
        assertTrue(
            loggedError.message.contains(REMOTE_LOGGER_PREFIX),
            "Expected message to contain '$REMOTE_LOGGER_PREFIX' prefix"
        )
        assertTrue(
            loggedError.message.contains(FAILED_TO_LOG_MESSAGE),
            "Expected message to contain error description"
        )
        assertEquals(testException, loggedError.throwable)
    }

    @Test
    fun `LoggerScope correctly prefixes messages with RemoteLogger-post`() = runTest(testDispatcher) {
        // Arrange
        every { mockCall.execute() } throws IOException("Test exception")

        // Act
        remoteLogger.log(Level.Error, "Test message", null)

        // Allow coroutine to complete
        advanceUntilIdle()

        // Assert
        assertTrue(errorLogger.logged.isNotEmpty())
        val loggedError = errorLogger.logged.first()
        assertTrue(
            loggedError.message.contains(REMOTE_LOGGER_PREFIX),
            "Expected message to contain '$REMOTE_LOGGER_PREFIX'"
        )
        assertTrue(
            loggedError.message.contains("post"),
            "Expected message to contain 'post' scope"
        )
    }

    @Test
    fun `isEnabled flag is set to false after HTTP failure`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("Service Unavailable")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act - First log should trigger the error
        remoteLogger.log(Level.Error, "First message", null)
        advanceUntilIdle()

        // Clear the logged messages
        errorLogger.logged.clear()

        // Act - Second log should be ignored due to isEnabled = false
        remoteLogger.log(Level.Error, "Second message", null)
        advanceUntilIdle()

        // Assert - No new error logs should be created for the second message
        // because remote logging is disabled
        assertTrue(
            errorLogger.logged.isEmpty(),
            "Expected no additional logs after isEnabled is set to false"
        )
    }

    @Test
    fun `isEnabled flag is set to false after IOException`() = runTest(testDispatcher) {
        // Arrange
        every { mockCall.execute() } throws IOException("Connection timeout")

        // Act - First log should trigger the error
        remoteLogger.log(Level.Error, "First message", null)
        advanceUntilIdle()

        // Clear the logged messages
        errorLogger.logged.clear()

        // Act - Second log should be ignored due to isEnabled = false
        remoteLogger.log(Level.Error, "Second message", null)
        advanceUntilIdle()

        // Assert - No new error logs should be created
        assertTrue(
            errorLogger.logged.isEmpty(),
            "Expected no additional logs after isEnabled is set to false"
        )
    }

    @Test
    fun `successful HTTP request does not log errors`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act
        remoteLogger.log(Level.Error, "Test message", null)
        advanceUntilIdle()

        // Assert
        assertTrue(
            errorLogger.logged.isEmpty(),
            "Expected no errors to be logged for successful HTTP request"
        )
    }

    @Test
    fun `logs below Warning level are not sent to remote logger`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act - Log with Debug level (below Warning)
        remoteLogger.log(Level.Debug, "Debug message", null)
        advanceUntilIdle()

        // Assert - No HTTP call should be made (verified by no error logs)
        assertTrue(errorLogger.logged.isEmpty())
    }

    @Test
    fun `error call on HTTP failure invokes externalLogScope logger`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(404)
            .message("Not Found")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act
        remoteLogger.log(Level.Warning, "Test warning", null)
        advanceUntilIdle()

        // Assert
        assertFalse(errorLogger.logged.isEmpty(), "Expected error logger to be invoked")
        val logged = errorLogger.logged.first()
        assertEquals(Level.Error, logged.level, "Expected error level for failed remote log")
    }

    @Test
    fun `error call on IOException invokes externalLogScope logger`() = runTest(testDispatcher) {
        // Arrange
        every { mockCall.execute() } throws IOException("Connection reset")

        // Act
        remoteLogger.log(Level.Error, "Test error", null)
        advanceUntilIdle()

        // Assert
        assertFalse(errorLogger.logged.isEmpty(), "Expected error logger to be invoked")
        val logged = errorLogger.logged.first()
        assertEquals(Level.Error, logged.level, "Expected error level for failed remote log")
        assertTrue(logged.throwable is IOException, "Expected IOException to be logged")
    }

    @Test
    fun `public constructor creates RemoteLogger and works correctly`() = runTest {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act - Use public constructor (which doesn't take dispatcher parameter)
        val logger = RemoteLogger(
            version = "1.0.0",
            okHttpClient = mockClient,
            errorLogger = errorLogger
        )

        // Log using the public constructor instance
        logger.log(Level.Error, "Test from public constructor", null)
        // Give async time to complete
        kotlinx.coroutines.delay(200)

        // Assert - Logger should be created successfully and no errors logged
        assertTrue(errorLogger.logged.isEmpty(), "Expected successful logging with public constructor")
    }

    @Test
    fun `Warning level maps to WARNING priority`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act - Log Warning level which should map to WARNING priority
        remoteLogger.log(Level.Warning, "Warning level test", null)
        advanceUntilIdle()

        // Assert - No error logs means successful remote logging
        assertTrue(errorLogger.logged.isEmpty(), "Warning level should be logged remotely")
    }

    @Test
    fun `level priority maps Error correctly`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act
        remoteLogger.log(Level.Error, "Error level test", null)
        advanceUntilIdle()

        // Assert - No error logs means successful remote logging
        assertTrue(errorLogger.logged.isEmpty(), "Error level should be logged remotely")
    }

    @Test
    fun `level priority maps Info correctly`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act
        remoteLogger.log(Level.Info, "Info level test", null)
        advanceUntilIdle()

        // Assert - No error logs means successful remote logging
        assertTrue(errorLogger.logged.isEmpty(), "Info level should be logged remotely")
    }

    @Test
    fun `level priority maps Verbose as default`() = runTest(testDispatcher) {
        // Arrange - Mock response shouldn't be called for Verbose level
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act - Verbose is below Warning threshold, should not trigger HTTP request
        remoteLogger.log(Level.Verbose, "Verbose message", null)
        advanceUntilIdle()

        // Assert - No error logs and no HTTP call made
        assertTrue(errorLogger.logged.isEmpty(), "Verbose level should be ignored")
    }

    @Test
    fun `log with throwable extracts file and line from stack trace`() = runTest(testDispatcher) {
        // Arrange
        val requestSlot = slot<Request>()
        every { mockClient.newCall(capture(requestSlot)) } returns mockCall

        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Create a throwable with a stack trace
        val throwable = try {
            throw RuntimeException("Test exception")
        } catch (e: RuntimeException) {
            e
        }

        // Act
        remoteLogger.log(Level.Error, "Error with throwable", throwable)
        advanceUntilIdle()

        // Assert - Request should contain data (no errors logged)
        assertTrue(errorLogger.logged.isEmpty(), "Should successfully log with throwable")
    }

    @Test
    fun `log with throwable with shallow stack trace handles gracefully`() = runTest(testDispatcher) {
        // Arrange
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Create a throwable with minimal stack trace
        val throwable = RuntimeException("Test")

        // Act - Even with minimal stack, should not crash
        remoteLogger.log(Level.Warning, "Warning with minimal throwable", throwable)
        advanceUntilIdle()

        // Assert
        assertTrue(errorLogger.logged.isEmpty(), "Should handle minimal stack trace")
    }

    @Test
    fun `all log levels below Warning are ignored`() = runTest(testDispatcher) {
        // Arrange - Set up mock but it shouldn't be called
        val mockResponse = Response.Builder()
            .request(Request.Builder().url(TEST_BASE_LOGGER_URL).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("".toResponseBody(TEXT_PLAIN_MEDIA_TYPE.toMediaType()))
            .build()

        every { mockCall.execute() } returns mockResponse

        // Act - Try logging with Debug and Verbose levels
        remoteLogger.log(Level.Debug, "Debug message", null)
        remoteLogger.log(Level.Verbose, "Verbose message", null)
        advanceUntilIdle()

        // Assert - No HTTP calls should be made
        assertTrue(errorLogger.logged.isEmpty(), "Debug and Verbose levels should be ignored")
    }

    @Test
    fun `logging when finalLoggerUrl is null does not make HTTP calls`() = runTest(testDispatcher) {
        // Arrange - Set finalLoggerUrl to null using reflection
        val finalLoggerUrlField = RemoteLogger::class.java.getDeclaredField(FINAL_LOGGER_URL_FIELD).apply {
            isAccessible = true
        }
        finalLoggerUrlField.set(null, null)

        // Create logger with null URL
        val logger = RemoteLogger(
            version = TEST_VERSION,
            okHttpClient = mockClient,
            errorLogger = errorLogger,
            dispatcher = testDispatcher
        )

        // Act - Try to log
        logger.log(Level.Error, "This should not be sent", null)
        advanceUntilIdle()

        // Assert - No errors should be logged (method returns early)
        assertTrue(errorLogger.logged.isEmpty(), "Should handle null URL gracefully")

        // Restore URL for other tests
        finalLoggerUrlField.set(null, TEST_LOGGER_URL)
    }

    @Test
    fun `logging when finalLoggerUrl is empty does not make HTTP calls`() = runTest(testDispatcher) {
        // Arrange - Set finalLoggerUrl to empty string using reflection
        val finalLoggerUrlField = RemoteLogger::class.java.getDeclaredField(FINAL_LOGGER_URL_FIELD).apply {
            isAccessible = true
        }
        finalLoggerUrlField.set(null, "")

        // Create logger with empty URL
        val logger = RemoteLogger(
            version = TEST_VERSION,
            okHttpClient = mockClient,
            errorLogger = errorLogger,
            dispatcher = testDispatcher
        )

        // Act - Try to log
        logger.log(Level.Error, "This should not be sent", null)
        advanceUntilIdle()

        // Assert - No errors should be logged (method returns early)
        assertTrue(errorLogger.logged.isEmpty(), "Should handle empty URL gracefully")

        // Restore URL for other tests
        finalLoggerUrlField.set(null, TEST_LOGGER_URL)
    }

    @Test
    fun `evaluateLoggerUrl with valid channels URL returns app logger-public URL`() {
        // Act
        val result = RemoteLogger.evaluateLoggerUrl("https://channels.na1.nice-incontact.com/chat/")

        // Assert
        assertEquals("https://app.na1.nice-incontact.com/logger-public", result)
    }

    @Test
    fun `evaluateLoggerUrl with different valid channels URL returns app logger-public URL`() {
        // Act
        val result = RemoteLogger.evaluateLoggerUrl("https://channels.test.example.com/chat/")

        // Assert
        assertEquals("https://app.test.example.com/logger-public", result)
    }

    @Test
    fun `evaluateLoggerUrl with malformed URL returns null`() {
        // Act
        val result = RemoteLogger.evaluateLoggerUrl("not-a-valid-url")

        // Assert
        assertNull(result, "Malformed URL should return null")
    }

    @Test
    fun `evaluateLoggerUrl with URL missing chat path returns null`() {
        // Act
        val result = RemoteLogger.evaluateLoggerUrl("https://channels.example.com/other/")

        // Assert
        assertNull(result, "URL without /chat/ path should return null")
    }

    @Test
    fun `evaluateLoggerUrl with non-matching pattern returns null`() {
        // Act - URL that doesn't match the expected https://app.../logger-public pattern
        val result = RemoteLogger.evaluateLoggerUrl("https://invalid-domain.com/chat/")

        // Assert
        assertNull(result, "Non-matching pattern should return null")
    }

    @Test
    fun `evaluateLoggerUrl with empty string returns null`() {
        // Act
        val result = RemoteLogger.evaluateLoggerUrl("")

        // Assert
        assertNull(result, "Empty URL should return null")
    }

    companion object {
        private const val TEST_VERSION = "1.0.0-test"
        private const val TEST_LOGGER_URL = "https://test.example.com/logger?brandId=123&program=android-dfo-chat"
        private const val TEST_BASE_LOGGER_URL = "https://test.example.com/logger"
        private const val TEST_FINGERPRINT = "test-fingerprint"
        private const val FINAL_LOGGER_URL_FIELD = "finalLoggerUrl"
        private const val DEVICE_FINGERPRINT_FIELD = "deviceFingerprint"
        private const val REMOTE_LOGGER_PREFIX = "RemoteLogger"
        private const val FAILED_TO_LOG_MESSAGE = "Failed to log message"
        private const val ERROR_LOGGED_MESSAGE = "Expected error to be logged to errorLogger"
        private const val TEXT_PLAIN_MEDIA_TYPE = "text/plain"
    }
}
