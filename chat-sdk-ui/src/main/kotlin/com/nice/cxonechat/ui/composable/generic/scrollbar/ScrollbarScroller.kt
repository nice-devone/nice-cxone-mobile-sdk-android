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

package com.nice.cxonechat.ui.composable.generic.scrollbar

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** A single movement request produced by a scrollbar gesture. */
internal sealed interface ScrollCommand {
    /** Move the content by [delta] pixels. */
    data class By(val delta: Float) : ScrollCommand

    /** Move the content so it sits at [offset], by whatever difference that takes when it runs. */
    data class To(val offset: Float) : ScrollCommand
}

/**
 * Applies one gesture's scroll requests in order, without overlapping.
 *
 * Every conversion from indicator space to content space reads the current geometry, so two
 * requests in flight at once fight each other. A gesture therefore opens a single user-priority
 * scroll and drains an unbounded queue inside it: requests are applied strictly in the order they
 * were made, and exactly one coroutine exists per gesture rather than one per pointer event.
 *
 * Opening the scroll at user priority is also what stops a fling still in flight — the mutation
 * mutex preempts it, so no separate stop step and no delay before the first jump are needed.
 */
internal class ScrollbarScroller(private val source: ScrollbarSource) {

    private var commands: Channel<ScrollCommand>? = null

    /**
     * Opens a scroll for a new gesture, ending any queue left over from a previous one.
     *
     * The returned job completes once [end] has been called and everything queued has been applied.
     */
    fun begin(scope: CoroutineScope): Job {
        end()
        val queue = Channel<ScrollCommand>(Channel.UNLIMITED)
        commands = queue
        return scope.launch {
            source.withUserScroll {
                for (command in queue) {
                    val delta = when (command) {
                        is ScrollCommand.By -> command.delta
                        // Resolved against the offset reached so far, so dragging tracks the finger
                        // instead of compounding the position it started from.
                        is ScrollCommand.To -> command.offset - source.offset
                    }
                    scrollBy(delta)
                }
            }
        }
    }

    /** Queues a request. Does nothing outside a gesture. */
    fun push(command: ScrollCommand) {
        commands?.trySend(command)
    }

    /** Closes the queue, letting the scroll finish once everything already queued has been applied. */
    fun end() {
        commands?.close()
        commands = null
    }
}
