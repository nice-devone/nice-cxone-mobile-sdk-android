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

package com.nice.cxonechat.ui.domain.usecase

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build.VERSION_CODES
import com.nice.cxonechat.Popup
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.data.ChatNotificationManager
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [VERSION_CODES.O])
internal class NotifyUpdateUseCaseTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val valueStorage = mockk<ValueStorage>(relaxed = true) {
        every { getStringSet(any()) } returns flowOf(emptySet())
    }
    private val notificationManager = ChatNotificationManager(context, valueStorage, LoggerNoop)
    private val useCase = NotifyUpdateUseCase(context, notificationManager, LoggerNoop)

    private val postedNotifications: List<Notification>
        get() = shadowOf(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).allNotifications

    private fun threadMock(canAddMoreMessages: Boolean = true) = Thread(
        chatThread = mockk<ChatThread>(relaxed = true) {
            every { id } returns UUID.randomUUID()
            every { messages } returns emptyList()
            every { this@mockk.canAddMoreMessages } returns canAddMoreMessages
            every { threadName } returns null
        },
        name = null,
    )

    @Test
    fun `notifyUpdate posts no notification when the thread has no unread messages`() = runTest {
        useCase.notifyUpdate(threadMock())

        assertTrue(
            "An update with no unread messages must not post a title-less, text-less notification",
            postedNotifications.isEmpty()
        )
    }

    @Test
    fun `notifyUpdate posts no notification for an archived thread`() = runTest {
        useCase.notifyUpdate(threadMock(canAddMoreMessages = false))

        assertTrue(postedNotifications.isEmpty())
    }

    @Test
    fun `notifySatisfactionSurvey posts a notification with a static title and the survey's text`() = runTest {
        val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true) {
            every { text } returns "How did we do today?"
        }

        useCase.notifySatisfactionSurvey(threadMock(), survey)

        val notification = postedNotifications.single()
        assertEquals(
            "Satisfaction Survey",
            notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString()
        )
        assertEquals(
            "How did we do today?",
            notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString()
        )
    }
}
