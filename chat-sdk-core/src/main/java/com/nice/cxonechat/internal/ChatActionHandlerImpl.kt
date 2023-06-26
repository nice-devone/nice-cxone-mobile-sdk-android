package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatActionHandler.OnPopupActionListener
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.enums.ActionType.CustomPopupBox
import com.nice.cxonechat.enums.EventType.FireProactiveAction
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback

internal class ChatActionHandlerImpl(
    chat: ChatWithParameters,
) : ChatActionHandler {

    private var latestParams: ParamsWithMetadata? = null
    private var popupListener: OnPopupActionListener? = null
    private val popupCancellable = chat.socketListener
        .addCallback<EventProactiveAction>(FireProactiveAction) { model ->
            val listener = popupListener
            val metadata = model.metadata
            if (model.type != CustomPopupBox) return@addCallback
            val variables = model.variables
            if (listener == null) {
                latestParams = ParamsWithMetadata(variables.orEmpty(), metadata)
                return@addCallback
            }
            if (variables != null) listener.onShowPopup(variables, metadata)
            latestParams = null
        }

    override fun onPopup(listener: OnPopupActionListener) {
        this.popupListener = listener
        val (params, metadata) = latestParams ?: return
        latestParams = null
        listener.onShowPopup(params, metadata)
    }

    override fun close() {
        popupListener = null
        popupCancellable.cancel()
    }

    private data class ParamsWithMetadata(
        val params: Map<String, Any?>,
        val metadata: ActionMetadata,
    )
}
