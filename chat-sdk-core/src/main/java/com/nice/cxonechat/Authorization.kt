/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat

/**
 * Authorization for OAuth use-cases.
 * Client should pass the SDK necessary information, so the _server_ can
 * successfully authenticate the user.
 */
@Public
interface Authorization {
    /**
     * Authentication code provided by -possibly- third party or your own server.
     */
    val code: String

    /**
     * Code verifier provided in conjunction with authentication code.
     */
    val verifier: String

    @Public
    companion object {
        @JvmSynthetic
        internal val None = Authorization("", "")

        /**
         * Create a new instance of Authorization with the indicated code and verifier.
         *
         * @param code Authentication code provided by -possibly- third party or your own server.
         * @param verifier Code verifier provided in conjunction with authentication code.
         */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(code: String, verifier: String): Authorization = AuthorizationImpl(code, verifier)
    }
}

/**
 * Authorization for OAuth use-cases.
 * Client should pass the SDK necessary information, so the _server_ can
 * successfully authenticate the user.
 *
 * @property code Authentication code provided by -possibly- third party or your own server.
 * @property verifier Code verifier provided in conjunction with authentication code.
 */
private data class AuthorizationImpl(override val code: String, override val verifier: String) : Authorization
