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

package com.nice.cxonechat.ui.data

import android.net.Uri
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.ui.viewmodel.AttachmentTarget
import com.nice.cxonechat.ui.viewmodel.ChatStateViewModel
import com.nice.cxonechat.ui.viewmodel.ChatThreadViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class ChatAttachmentHandlerTest {

    /**
     * The target must describe the session the pick was made in, so it has to be read before the wait for
     * Prepared: once released, the chat may already belong to the next user's session.
     */
    @Test
    fun `addAttachment records the attachment target before waiting for the chat to be prepared`() = runTest(timeout = 10.seconds) {
        val state = MutableStateFlow(ChatState.Initial)
        val chatStateViewModel = mockk<ChatStateViewModel> { every { this@mockk.state } returns state }
        val target = AttachmentTarget(sessionGeneration = 0, handler = null, threadId = null)
        val viewModel = mockk<ChatThreadViewModel>(relaxed = true) {
            every { currentAttachmentTarget() } returns target
        }
        val handler = ChatAttachmentHandler(mockk(relaxed = true), LoggerNoop, lazyOf(viewModel), chatStateViewModel)
        val picked = listOf(mockk<Uri>(relaxed = true))

        val adding = launch { handler.addAttachment(picked) }
        runCurrent()

        verify(exactly = 1) { viewModel.currentAttachmentTarget() }
        verify(exactly = 0) { viewModel.addPendingAttachments(any(), any()) }

        state.value = ChatState.Prepared
        adding.join()

        verify(exactly = 1) { viewModel.addPendingAttachments(picked, target) }
    }
}
