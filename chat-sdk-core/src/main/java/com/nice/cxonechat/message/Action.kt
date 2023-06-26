package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Actionable item to display as part of a message.
 */
@Public
interface Action {
    /**
     * Display a button which *may* have an associated icon.
     * If the button is selected integrating application should send [OutboundMessage]
     * with the supplied [text] and [postback].
     */
    @Public
    interface ReplyButton : Action {
        /** Text to display on a button.  */
        val text: String

        /** Postback to be sent as part of the [OutboundMessage] if the button is selected. */
        val postback: String?

        /** optional media/image to display with the media. */
        val media: Media?

        /** optional longer more descriptive text to display with the button. */
        val description: String?
    }
}
