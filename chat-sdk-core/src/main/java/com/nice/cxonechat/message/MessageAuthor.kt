package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Author of a given [Message]. This field is backed by, or rather converted from,
 * different implementations depending on the [Message.direction] or possibly
 * other factors.
 *
 * If the message doesn't provide an actor(author), default values will be used.
 * These default values are thereafter the identical for the lifetime of the
 * process. Do not rely on the stability of IDs for example after the process
 * finishes.
 * */
@Public
abstract class MessageAuthor {
    /**
     * Id of an author. Converted implementations can have different implementations
     * for ids, though they are all converted to string for convenience.
     * */
    abstract val id: String

    /**
     * First name of given actor.
     *
     * @see name
     * */
    abstract val firstName: String

    /**
     * Last name of given actor.
     *
     * @see name
     * */
    abstract val lastName: String

    /**
     * Merges [firstName] and [lastName] in this order, separated by a space.
     * If both values are empty, then returns an empty string.
     * */
    val name
        get() = "$firstName $lastName".trim()

}
