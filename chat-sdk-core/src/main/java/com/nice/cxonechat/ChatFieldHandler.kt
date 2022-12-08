package com.nice.cxonechat

/**
 * Handler permitting to add new fields to the instance it was created from.
 *
 * The instance may or may not update values in the upstream object,
 * resulting in runtime behavior changes. These changes mainly regard to the
 * requests being modified by these fields.
 * */
@Public
interface ChatFieldHandler {

    /**
     * Adds specified [fields] to the instance. If requested on a thread that's
     * newly created the fields may be lost until a first message is sent.
     *
     * Client should always ensure thread (if applicable) exists before making
     * changes to it. Threads are generally created by sending a first message
     * to it.
     * */
    fun add(fields: Map<String, String>)

}
