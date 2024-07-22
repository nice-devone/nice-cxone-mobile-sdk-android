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

package com.nice.cxonechat.sample.utilities

import android.os.Build
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import android.util.Base64 as AndroidUtilBase64

/** PKCE authentication details. */
object PKCE {
    private const val BITS_IN_CHAR = 6
    private const val BITS_IN_OCTET = 8
    private const val MIN_CODE_VERIFIER_LENGTH = 43
    private const val MAX_CODE_VERIFIER_LENGTH = 128

    /**
     * Generate a code verifier and challenge pair with a maximum length of [length].
     *
     * @param length required length of verifier.  Must be between [MIN_CODE_VERIFIER_LENGTH]
     * and [MAX_CODE_VERIFIER_LENGTH].  Defaults to [MAX_CODE_VERIFIER_LENGTH]
     * @return [Pair] of strings containing the code verifier and the code challenge.
     */
    fun generateCodeVerifier(length: Int = MAX_CODE_VERIFIER_LENGTH): Pair<String, String> {
        check(length in MIN_CODE_VERIFIER_LENGTH..MAX_CODE_VERIFIER_LENGTH) {
            "Invalid length for code verifier. Must be greater than $MIN_CODE_VERIFIER_LENGTH and less than $MAX_CODE_VERIFIER_LENGTH."
        }
        val octetCount = length * BITS_IN_CHAR / BITS_IN_OCTET
        val codeVerifier = encodeBase64URLString(generateRandomOctets(octetCount))
        return codeVerifier to generateCodeChallenge(codeVerifier)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val challengeBytes = codeVerifier.toByteArray()
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(challengeBytes, 0, challengeBytes.size)
        return encodeBase64URLString(messageDigest.digest())
    }

    private fun generateRandomOctets(octetCount: Int): ByteArray {
        val octets = ByteArray(octetCount)
        val secureRandom = SecureRandom()
        secureRandom.nextBytes(octets)
        return octets
    }

    private fun encodeBase64URLString(octets: ByteArray): String {
        val encoded = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val flags = AndroidUtilBase64.NO_WRAP or AndroidUtilBase64.URL_SAFE
            AndroidUtilBase64.encodeToString(octets, flags)
        } else {
            Base64.getUrlEncoder().encodeToString(octets)
        }
        return encoded
            .replace("=", "") // FIXME this can be removed if encoder is set to NO_PADDING
            .replace("+", "-") // FIXME this looks useless, url-safe encoder should not use this char
            .replace("/", "_") // FIXME same as above
    }
}
