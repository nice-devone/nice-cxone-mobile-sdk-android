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

package com.nice.cxonechat.sample.viewModel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandlerActions.conversion
import com.nice.cxonechat.ChatEventHandlerActions.pageView
import com.nice.cxonechat.ChatEventHandlerActions.pageViewEnded
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.info
import com.nice.cxonechat.log.scope
import java.util.Date

/**
 * Manage page view events.
 *
 * @param chatProvider [ChatInstanceProvider] to use for actually sending page view
 * and page view ended events.
 * @param logger [Logger] used as base for [LoggerScope].
 */
class AnalyticsHandler(
    private val chatProvider: ChatInstanceProvider,
    logger: Logger,
) : LoggerScope by LoggerScope(TAG, logger) {
    private val events: ChatEventHandler?
        get() = chatProvider.chat?.events()

    /**
     * Recorded details of pageView and pageViewEnded events.
     *
     * @property title title of viewed page.
     * @property url url of viewed page.
     */
    @Immutable
    data class PageInfo(val title: String, val url: String)

    private var lastPageInfo: PageInfo? = null
    private var pageViewEndedNeeded = false

    /** the associated activity has received onResume. */
    fun onResume() {
        lastPageInfo?.let(::sendPageView)
    }

    /** the associated activity has received onPause. */
    fun onPause() {
        sendPageViewEnded()
    }

    /**
     * Send a conversion event to the analytics service.
     *
     * @param type application-specific "type" of conversion.
     * @param amount dollar amount of conversion.
     * @param date date of conversion, defaults to now.
     */
    fun sendConversion(type: String, amount: Double, date: Date = Date()) {
        events?.conversion(type, amount, date)
    }

    /**
     * Record PageView and PageViewEnded event handler with a given page title and url.
     *
     * @see [SendPageView]
     *
     * @param title Unique page title to be recorded.
     * @param url Unique page url to be recorded.
     * @param keys Any additional key values that could signify a page change.  Examples might be
     * values that trigger dialogs which should be considered distinct page views such as
     * [LoginDialog].
     */
    @Composable
    fun SendPageView(title: String, url: String, vararg keys: Any?) {
        SendPageView(pageInfo = PageInfo(title, url), *keys)
    }

    /**
     * Record page view info to appropriately generate pageView and pageViewEnded events.
     *
     * @param pageInfo Page info (title and url) to be recorded.  If null is passed, no page view
     * should be generated, presumably because it is being separately handled by a dialog considered
     * a distinct page view.
     * @param keys Any additional key values that could signify a page change.  Examples might be
     * values that trigger dialogs which should be considered distinct page views such as
     * [LoginDialog].
     */
    @Composable
    fun SendPageView(pageInfo: PageInfo?, vararg keys: Any?) {
        DisposableEffect(pageInfo, *keys) {
            if (pageInfo != lastPageInfo) {
                sendPageViewEnded()
            }

            if (pageInfo != null) {
                sendPageView(pageInfo, Date())
            }

            onDispose {
                if (lastPageInfo == pageInfo) {
                    sendPageViewEnded()
                }
            }
        }
    }

    private fun sendPageView(pageInfo: PageInfo, date: Date = Date()) = scope("sendPageView") {
        info("sendPageView(title=${pageInfo.title}, url=${pageInfo.url})")
        events?.pageView(pageInfo.title, pageInfo.url, date)
        lastPageInfo = pageInfo
        pageViewEndedNeeded = true
    }

    private fun sendPageViewEnded() = scope("sendPageViewEnded") {
        val last = lastPageInfo ?: return

        if (pageViewEndedNeeded) {
            info("sendPageViewEnded(title=${last.title}, url=${last.url})")
            events?.pageViewEnded(last.title, last.url, Date())
            pageViewEndedNeeded = false
        }
    }

    companion object {
        private const val TAG = "PageViewHandler"
    }
}
