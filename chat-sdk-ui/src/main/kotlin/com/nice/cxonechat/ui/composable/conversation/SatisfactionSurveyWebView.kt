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

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Message
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import java.io.ByteArrayInputStream

/**
 * Overrides [SatisfactionSurveyWebView]'s content. Screenshot tests can't meaningfully render a
 * real WebView under Robolectric, so they provide a solid-color/fake-page replacement here instead
 * of exercising the real implementation; production code never overrides this.
 */
internal val LocalSurveyWebViewContent =
    compositionLocalOf<(@Composable (surveyUrl: String, modifier: Modifier) -> Unit)?> { null }

/**
 * Loads the Satisfaction Survey page: HTTPS only, cross-origin top-level navigation blocked only
 * on an actual user tap (not a programmatic redirect), iframes never intercepted, camera/mic
 * denied, no new windows/tabs.
 */
@SuppressLint("SetJavaScriptEnabled") // The survey page is backend-controlled and may require script to function.
@Composable
internal fun SatisfactionSurveyWebView(surveyUrl: String, modifier: Modifier = Modifier) {
    val override = LocalSurveyWebViewContent.current
    if (override != null) {
        override(surveyUrl, modifier)
        return
    }
    // AndroidView's factory runs once and has no update step, so without this key, a popup
    // instance that somehow received a new surveyUrl while still composed would keep showing the
    // old page under the old origin instead of loading the new one.
    key(surveyUrl) {
        val allowedOrigin = remember(surveyUrl) { surveyUrl.toUri().origin() }
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .testTag("satisfaction_survey_web_view"),
            factory = { context ->
                // WebView must be wrapped in a container view, otherwise it fails to load local JS files.
                FrameLayout(context).apply {
                    val webView = WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        // Must be true for DenyMediaPermissionsWebChromeClient.onCreateWindow() to be
                        // called at all - with multi-window support off, Chromium opens a target="_blank"
                        // or window.open() request as a same-origin in-place navigation instead.
                        settings.setSupportMultipleWindows(true)
                        webViewClient = SurveyWebViewClient(allowedOrigin)
                        webChromeClient = DenyMediaPermissionsWebChromeClient()
                        // shouldOverrideUrlLoading is not called for the initial load, and
                        // shouldInterceptRequest is not called for javascript:/blob:/data: URLs,
                        // so check HTTPS here too.
                        loadUrl(if (allowedOrigin != null) surveyUrl else "about:blank")
                    }
                    addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                }
            },
            onRelease = { root -> (root.getChildAt(0) as? WebView)?.destroy() },
        )
    }
}

internal const val HTTPS_SCHEME = "https"

internal class SurveyWebViewClient(initialOrigin: String?) : WebViewClient() {

    // Updated only when a main-frame navigation actually commits (onPageCommitVisible below), not
    // merely when it's allowed to start - an allowed programmatic cross-origin navigation that
    // then fails or is cancelled leaves the old page visible, so adopting its origin early would
    // evaluate the still-visible page's taps against a destination it never reached. Volatile
    // rather than synchronized: shouldInterceptRequest reads this from a background thread while
    // onPageCommitVisible writes it from the UI thread, but the write never depends on a prior
    // read, so plain visibility is enough.
    @Volatile
    private var allowedOrigin: String? = initialOrigin

    private fun shouldBlockMainFrameRequest(request: WebResourceRequest): Boolean {
        val isHttps = request.url.scheme.equals(HTTPS_SCHEME, ignoreCase = true)
        val isSameOrigin = allowedOrigin != null && request.url.origin() == allowedOrigin
        return request.isForMainFrame && (!isHttps || !isSameOrigin && request.hasGesture())
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        shouldBlockMainFrameRequest(request)

    // Not called for the initial loadUrl() or a POST navigation, unlike shouldOverrideUrlLoading -
    // runs for every request instead, before it reaches the network. A block here shows a blank
    // page rather than leaving the current one, since the frame does navigate to this response.
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
        if (shouldBlockMainFrameRequest(request)) BLOCKED_RESPONSE else null

    // Fires once a main-frame navigation has actually committed and is about to be drawn - the
    // right moment to adopt its origin, since a navigation that fails or is cancelled never
    // reaches here and correctly leaves the previously-committed origin as the allowed one.
    override fun onPageCommitVisible(view: WebView, url: String) {
        url.toUri().origin()?.let { allowedOrigin = it }
    }

    private companion object {
        val BLOCKED_RESPONSE = WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
    }
}

internal class DenyMediaPermissionsWebChromeClient : WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest) {
        request.deny()
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?,
    ): Boolean = false
}

/** HTTPS-only origin (scheme+host+port); any other scheme, or a missing host, has no valid origin. */
private fun Uri.origin(): String? {
    val host = host?.takeIf { it.isNotEmpty() }
    val isHttps = scheme.equals(HTTPS_SCHEME, ignoreCase = true)
    return if (host != null && isHttps) "https://$host:${port.takeIf { it != -1 } ?: 443}" else null
}
