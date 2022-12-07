package com.nice.cxonechat.enums

/**
 * The list of all statuses on a contact.
 */
internal enum class ContactStatus(val value: String) {
    /** The contact is newly opened. */
    New("new"),

    /** The contact is currently open. */
    Open("open"),

    /** The contact is pending. */
    Pending("pending"),

    /** The contact has been escalated. */
    Escalated("escalated"),

    /** The contact has been resolved. */
    Resolved("resolved"),

    /** The contact is closed. */
    Closed("closed"),

    /** The contact contains some unknown status string. */
    Unknown("???");
}
