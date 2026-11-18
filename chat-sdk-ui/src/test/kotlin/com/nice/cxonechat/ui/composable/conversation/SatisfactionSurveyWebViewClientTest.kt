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

package com.nice.cxonechat.ui.composable.conversation

import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import androidx.core.net.toUri
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers [SurveyWebViewClient]'s navigation policy and [DenyMediaPermissionsWebChromeClient]'s
 * capability denial directly - both are plain request/decision logic that needs no real WebView.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
internal class SatisfactionSurveyWebViewClientTest {

    private val initialOrigin = "https://survey.example.com:443"

    private fun requestFor(url: String, isForMainFrame: Boolean = true, hasGesture: Boolean = false): WebResourceRequest =
        mockk {
            every { this@mockk.url } returns url.toUri()
            every { this@mockk.isForMainFrame } returns isForMainFrame
            every { this@mockk.hasGesture() } returns hasGesture
        }

    @Test
    fun `a non-HTTPS main-frame navigation is blocked`() {
        val client = SurveyWebViewClient(initialOrigin)
        val request = requestFor("http://survey.example.com/page")

        assertTrue(client.shouldOverrideUrlLoading(mockk(relaxed = true), request))
    }

    @Test
    fun `a gesture-driven cross-origin navigation is blocked`() {
        val client = SurveyWebViewClient(initialOrigin)
        val request = requestFor("https://attacker.example.com/page", hasGesture = true)

        assertTrue(client.shouldOverrideUrlLoading(mockk(relaxed = true), request))
    }

    @Test
    fun `a programmatic cross-origin redirect is allowed and becomes the new committed origin once it commits`() {
        val client = SurveyWebViewClient(initialOrigin)
        val redirect = requestFor("https://login.example.com/bounce", hasGesture = false)

        assertTrue(!client.shouldOverrideUrlLoading(mockk(relaxed = true), redirect))
        client.onPageCommitVisible(mockk(relaxed = true), "https://login.example.com/bounce")

        // A later user tap on the redirected-to origin must now be treated as same-origin.
        val tapOnNewOrigin = requestFor("https://login.example.com/next", hasGesture = true)
        assertTrue(!client.shouldOverrideUrlLoading(mockk(relaxed = true), tapOnNewOrigin))
    }

    @Test
    fun `an allowed cross-origin navigation that never commits does not change the allowed origin`() {
        val client = SurveyWebViewClient(initialOrigin)
        val redirect = requestFor("https://login.example.com/bounce", hasGesture = false)

        // Allowed to start (no gesture), but the navigation fails/is cancelled before committing -
        // onPageCommitVisible is deliberately not called here, simulating that.
        assertTrue(!client.shouldOverrideUrlLoading(mockk(relaxed = true), redirect))

        // The still-visible original page's own origin must still be treated as same-origin.
        val tapOnOriginalOrigin = requestFor("https://survey.example.com/other", hasGesture = true)
        assertTrue(!client.shouldOverrideUrlLoading(mockk(relaxed = true), tapOnOriginalOrigin))

        // The never-committed destination must still be treated as cross-origin.
        val tapOnUncommittedOrigin = requestFor("https://login.example.com/next", hasGesture = true)
        assertTrue(client.shouldOverrideUrlLoading(mockk(relaxed = true), tapOnUncommittedOrigin))
    }

    @Test
    fun `a blocked cross-origin navigation does not become the committed origin`() {
        val client = SurveyWebViewClient(initialOrigin)
        val blockedTap = requestFor("https://attacker.example.com/page", hasGesture = true)
        assertTrue(client.shouldOverrideUrlLoading(mockk(relaxed = true), blockedTap))

        // The failed navigation must not have committed - a later tap on that same origin is still blocked.
        val secondTap = requestFor("https://attacker.example.com/other", hasGesture = true)
        assertTrue(client.shouldOverrideUrlLoading(mockk(relaxed = true), secondTap))
    }

    @Test
    fun `a subframe request is never blocked regardless of scheme or origin`() {
        val client = SurveyWebViewClient(initialOrigin)
        val request = requestFor("http://attacker.example.com/iframe", isForMainFrame = false, hasGesture = true)

        assertTrue(!client.shouldOverrideUrlLoading(mockk(relaxed = true), request))
        assertNull(client.shouldInterceptRequest(mockk(relaxed = true), request))
    }

    @Test
    fun `shouldInterceptRequest blocks the same requests shouldOverrideUrlLoading blocks`() {
        val client = SurveyWebViewClient(initialOrigin)
        val request = requestFor("https://attacker.example.com/page", hasGesture = true)

        assertTrue(client.shouldInterceptRequest(mockk(relaxed = true), request) != null)
    }

    @Test
    fun `media permission requests are always denied`() {
        val request = mockk<PermissionRequest>(relaxed = true)

        DenyMediaPermissionsWebChromeClient().onPermissionRequest(request)

        verify { request.deny() }
    }

    @Test
    fun `new windows are always denied`() {
        val denied = DenyMediaPermissionsWebChromeClient().onCreateWindow(
            view = mockk(relaxed = true),
            isDialog = false,
            isUserGesture = true,
            resultMsg = null,
        )

        assertTrue(!denied)
    }
}
