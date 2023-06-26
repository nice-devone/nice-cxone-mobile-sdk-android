package com.nice.cxonechat.message

import com.nice.cxonechat.Public
import java.util.Date

/**
 * Otherwise uncategorizable properties of a message.
 * */
@Public
abstract class MessageMetadata {
    /**
     * The date at which the message was read.
     * Defaults to null if the message is freshly sent or delivered.
     * */
    abstract val readAt: Date?
}
