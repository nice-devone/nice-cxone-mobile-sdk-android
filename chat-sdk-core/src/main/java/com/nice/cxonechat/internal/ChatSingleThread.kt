package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.thread.ChatThreadState.Ready

/**
 * This implementation of [com.nice.cxonechat.Chat] adds behavior which triggers early thread recovery if there is an
 * existing thread.
 * Once the [origin] [com.nice.cxonechat.Chat.connect] is finished, the mandatory [com.nice.cxonechat.Chat.threads] call
 * is performed and the first existing thread is then `refreshed` once it's metadata are loaded.
 * In order for these tasks to be of any use to the [com.nice.cxonechat.Chat] user, the [com.nice.cxonechat.Chat] has to
 * memoize both the [com.nice.cxonechat.ChatThreadsHandler] and the [com.nice.cxonechat.ChatThreadHandler].
 *
 * @param origin Existing implementation of [ChatWithParameters] used for delegation.
 */
internal class ChatSingleThread(private val origin: ChatWithParameters) : ChatWithParameters by origin {

    private var recoverCalled: Boolean = false

    override fun connect(): Cancellable {
        origin.connect()
        recoverCalled = false
        return tryToRecoverThread()
    }

    /**
     * Attempts to recover the single thread if it exists and notifies [com.nice.cxonechat.ChatStateListener] once
     * the task is done.
     */
    private fun tryToRecoverThread() = origin.entrails.threading.background {
        var threadsCancellable: Cancellable? = null
        val threadsHandler = origin.threads()
        threadsCancellable = threadsHandler.threads { threadList ->
            val threadHandler = threadList.firstOrNull()?.let(threadsHandler::thread)
            val threadState = threadHandler?.get()?.threadState
            if (threadHandler != null && threadState !== Ready && threadState !== Pending) {
                recoverThread(threadHandler)
            } else {
                // Either there is no thread or the thread is already recovered or it was user created.
                origin.chatStateListener?.onReady()
            }
            // Cleanup after the first thread list update.
            threadsCancellable?.cancel()
        }
        // Assuming that `threadsHandler.refresh` is called as part of the `threads` method.
    }

    private fun recoverThread(threadHandler: ChatThreadHandler) {
        var threadHandlerCancellable: Cancellable? = null
        threadHandlerCancellable = threadHandler.get { thread ->
            if (thread.threadState === Ready) {
                // The thread was recovered, signal listener and cleanup.
                origin.chatStateListener?.onReady()
                threadHandlerCancellable?.cancel()
            } else {
                // The metadata were just loaded, recover the thread.
                requestRecoverThread(threadHandler)
            }
        }
        // Recover the thread if the metadata are already Loaded.
        requestRecoverThread(threadHandler)
    }

    private fun requestRecoverThread(threadHandler: ChatThreadHandler) {
        if (!recoverCalled && threadHandler.get().threadState === Loaded) {
            recoverCalled = true
            threadHandler.refresh()
        }
    }
}
