package com.nice.cxonechat

import com.nice.cxonechat.message.ContentDescriptor
import java.util.UUID

/**
 * Manages all the necessary bits required for service messages. This newly
 * created object influences changes in parent [ChatThreadHandler] in number
 * of ways. Though it does not modify the parent directly, it can force some
 * events to happen which in-turn update the data.
 *
 * It has no side-effects attached to it.
 * */
@Public
interface ChatThreadMessageHandler {

    /**
     * Notifies server that we need to load more messages. This expects that
     * the instance provided by parent [ChatThreadHandler] has the most
     * up-to-date data. Be warned that calling this method on a thread that
     * has not been updated has undefined consequences. It can result for
     * example in message duplication.
     *
     * Please note that you should always call [ChatThreadHandler.get] with
     * a callback for this method to work. In any other case, this results in
     * undefined behavior.
     *
     * Call only if client reaches the top (or bottom, depending on the view
     * you've chosen) of the loaded messages after having them loaded by
     * [ChatThreadHandler.get].
     *
     * If [ChatThreadHandler.get] returns repeatedly empty list of messages,
     * the thread has probably no more messages left to load, therefore
     * calling this method makes no sense as it will again update the thread
     * with an empty list.
     * */
    fun loadMore()

    /**
     * Sends a message and optionally notifies the client about the message
     * being processed or sent.
     *
     * If the message has been processed but not sent within reasonable
     * amount of time, the client is permitted to retry.
     *
     * Execution (processing) of a given message is immediately moved to a
     * background thread, though [listener] will always be invoked on a
     * foreground thread.
     * */
    fun send(
        message: String,
        listener: OnMessageTransferListener? = null,
    )

    /**
     * It extends the functionality of [send] by handling upload of
     * provided attachments.
     *
     * If attachment misses file name, the file is named to "document"
     * upon being sent to the server. Please take care to provide localized
     * file names if you want to display them to the user.
     *
     * The upload of files is performed at most **once** before subsequent
     * processing of the message and sending it to the server. If the file
     * call succeeds it's cached internally to avoid doubling uploads.
     * Therefore subsequent calls (if the primary were to fail) are much
     * faster. Also if the user tries to send the same attachment again,
     * it will not be reuploaded instead it's referenced by the origin
     * upload. This cache is active as long as the [ChatBuilder] instance
     * remains the same. Reinitializing the [Chat] doesn't clear the cache.
     *
     * If any upload of any attachment fails by connection error, [listener]
     * will **not** be invoked for even processing triggers. The error is
     * muted and consumed within the thread.
     *
     * If any upload of any attachment fails by server error (returns but an
     * empty body), then the attachment is skipped and execution continues.
     *
     * @see send
     * */
    fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String = "",
        listener: OnMessageTransferListener? = null,
    )

    /**
     * Listener for simple inline id notifications
     * */
    @Public
    fun interface OnUUIDListener {
        /**
         * Notifications are based on the parent method. Refer to the caller.
         * */
        fun onTriggered(id: UUID)
    }

    /**
     * Listener allowing to receive transfer notifications.
     *
     * @see send
     * */
    @Public
    interface OnMessageTransferListener {
        /**
         * Notifies about the message being processed. This generally means
         * that the library finished all the necessary steps to send it to
         * the server.
         *
         * @see send
         * */
        fun onProcessed(id: UUID) = Unit

        /**
         * Notifies about the message being sent. This means that it was
         * dispatched from this device, but doesn't mean that is was
         * delivered to the server.
         *
         * @see send
         * */
        fun onSent(id: UUID) = Unit

        @Public
        companion object {

            /**
             * Helper method to create a compound [OnMessageTransferListener]
             *
             * @see [OnMessageTransferListener.onProcessed]
             * @see [OnMessageTransferListener.onSent]
             * */
            @JvmName("createFrom")
            @JvmStatic
            operator fun invoke(
                onProcessed: OnUUIDListener? = null,
                onSent: OnUUIDListener? = null,
            ) = object : OnMessageTransferListener {
                override fun onProcessed(id: UUID) = onProcessed?.onTriggered(id) ?: Unit
                override fun onSent(id: UUID) = onSent?.onTriggered(id) ?: Unit
            }

        }
    }

}
