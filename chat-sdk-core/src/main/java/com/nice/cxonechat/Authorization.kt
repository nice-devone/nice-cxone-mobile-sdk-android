package com.nice.cxonechat

/**
 * Authorization for OAuth use cases. Client should pass the SDK necessary
 * information so the _server_ can successfully authenticate the user.
 * */
@Public
data class Authorization(
    /**
     * Authentication code provided by -possibly- third party or your own server.
     * */
    val code: String,
    /**
     * Code verifier provided in conjunction with authentication code.
     * */
    val verifier: String,
) {

    companion object {

        internal val None = Authorization("", "")

    }

}
