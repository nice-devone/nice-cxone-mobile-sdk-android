/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.sample.data.models

import com.nice.cxonechat.sample.data.models.LaunchExtras.BASE_URL
import com.nice.cxonechat.sample.data.models.LaunchExtras.BRAND_ID
import com.nice.cxonechat.sample.data.models.LaunchExtras.CHANNEL_ID
import com.nice.cxonechat.sample.data.models.LaunchExtras.CHAT_URL
import com.nice.cxonechat.sample.data.models.LaunchExtras.CUSTOMER_ID
import com.nice.cxonechat.sample.data.models.LaunchExtras.ENVIRONMENT
import com.nice.cxonechat.sample.data.models.LaunchExtras.ENVIRONMENT_HOST
import com.nice.cxonechat.sample.data.models.LaunchExtras.ENV_LOCATION
import com.nice.cxonechat.sample.data.models.LaunchExtras.ENV_NAME
import com.nice.cxonechat.sample.data.models.LaunchExtras.FIRST_NAME
import com.nice.cxonechat.sample.data.models.LaunchExtras.LAST_NAME
import com.nice.cxonechat.sample.data.models.LaunchExtras.LOGGER_URL
import com.nice.cxonechat.sample.data.models.LaunchExtras.OPEN_CHAT
import com.nice.cxonechat.sample.data.models.LaunchExtras.ORIGIN_HEADER
import com.nice.cxonechat.sample.data.models.LaunchExtras.SOCKET_URL
import com.nice.cxonechat.sample.data.models.LaunchExtras.TOKEN_URL
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.Malformed
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.None
import com.nice.cxonechat.sample.data.models.StoreLaunchRequestResult.Supplied
import com.nice.cxonechat.sample.utilities.ExtrasSource

/** Names of the intent extras understood by the automated launch path. */
object LaunchExtras {
    /** Environment name, or [StoreEnvironment.CUSTOM]. */
    const val ENVIRONMENT = "environment"

    /** Host stem a custom environment's urls are derived from, e.g. `de-na1.niceincontact.com`. */
    const val ENVIRONMENT_HOST = "environmentHost"

    /** Brand id, which must be supplied as a long (`--el`). */
    const val BRAND_ID = "brandId"

    /** Channel id. */
    const val CHANNEL_ID = "channelId"

    /** Given name of the chat user. */
    const val FIRST_NAME = "firstName"

    /** Family name of the chat user. */
    const val LAST_NAME = "lastName"

    /** Customer id of the chat user. */
    const val CUSTOMER_ID = "customerId"

    /** Whether the chat should be opened once it is ready. Defaults to true. */
    const val OPEN_CHAT = "openChat"

    /** Name of a custom environment. */
    const val ENV_NAME = "envName"

    /** Physical location of a custom environment. */
    const val ENV_LOCATION = "envLocation"

    /** Base url of a custom environment. */
    const val BASE_URL = "baseUrl"

    /** Socket url of a custom environment. */
    const val SOCKET_URL = "socketUrl"

    /** Origin header of a custom environment. */
    const val ORIGIN_HEADER = "originHeader"

    /** Chat url of a custom environment. */
    const val CHAT_URL = "chatUrl"

    /** Token url of a custom environment. */
    const val TOKEN_URL = "tokenUrl"

    /** Logger url of a custom environment, which is optional. */
    const val LOGGER_URL = "loggerUrl"
}

/**
 * Chat user applied when a launch intent supplies a channel but no name.
 *
 * A channel without authorization leaves the sample app on its login dialog until it has a user
 * name, which would strand an unattended run. These stand in so that doesn't happen, and are
 * deliberately recognisable as an automated contact in the agent console.
 */
object DefaultChatUser {
    /** Given name of the stand-in chat user. */
    const val FIRST_NAME = "QA"

    /** Family name of the stand-in chat user. */
    const val LAST_NAME = "Automation"
}

/**
 * The environment a launch intent asked for, before it has been looked up.
 *
 * Resolution is deliberately separate: the intent is read as the activity is created, which is
 * before the bundled environment definitions have been loaded.
 */
sealed interface RequestedEnvironment {
    /**
     * An environment identified by name, to be looked up when the definitions are available.
     *
     * @param name Name as supplied on the intent.
     */
    data class Named(val name: String) : RequestedEnvironment

    /**
     * An environment described entirely by the intent, so nothing needs looking up.
     *
     * @param environment Definition built from the intent.
     */
    data class Custom(val environment: SdkEnvironment) : RequestedEnvironment
}

/**
 * The channel a launch intent asked to connect to.
 *
 * Identified by brand and channel rather than by the display name of one of the bundled
 * configurations: those names are human-readable labels which are expected to change, so they make a
 * poor contract for an automated run.
 *
 * @param environment Environment hosting the channel.
 * @param brandId Brand to connect to.
 * @param channelId Channel to connect to.
 */
data class SdkConfigurationRequest(
    val environment: RequestedEnvironment,
    val brandId: Long,
    val channelId: String,
) {
    /**
     * The requested channel, as the sample app stores it.
     *
     * @param resolved Environment this request's [environment] was looked up to.
     */
    fun toSdkConfiguration(resolved: SdkEnvironment) = SdkConfiguration(
        name = resolved.name,
        environment = resolved,
        brandId = brandId,
        channelId = channelId,
    )
}

/**
 * Everything the automated launch path can be asked to do, as supplied by a launch intent.
 *
 * @param configuration Channel to connect to, or null to keep whatever is already persisted.
 * @param loginData User to sign in as, or null to leave the user unset.
 * @param openChat Whether the chat should be opened once it is ready.
 */
data class StoreLaunchRequest(
    val configuration: SdkConfigurationRequest?,
    val loginData: LoginData?,
    val openChat: Boolean,
)

/** Outcome of reading a [StoreLaunchRequest] from a set of intent extras. */
sealed interface StoreLaunchRequestResult {
    /** The intent carried no automated launch instructions at all. */
    data object None : StoreLaunchRequestResult

    /**
     * The intent carried a usable request.
     *
     * @param request Request which was read.
     */
    data class Supplied(val request: StoreLaunchRequest) : StoreLaunchRequestResult

    /**
     * The intent tried to supply a request but it could not be used.
     *
     * @param reason Human-readable explanation, intended for the log so that whoever composed the
     * `adb shell am start` command can see what was wrong with it.
     */
    data class Malformed(val reason: String) : StoreLaunchRequestResult
}

/**
 * Read a [StoreLaunchRequest] from [source].
 *
 * Deliberately strict: anything which looks like an attempt to supply a configuration but doesn't
 * add up is reported as [Malformed] rather than partially applied, so a mistyped command fails
 * visibly instead of connecting somewhere unexpected.
 *
 * @param source Extras to read from.
 * @return What the extras asked for.
 */
fun parseStoreLaunchRequest(source: ExtrasSource): StoreLaunchRequestResult {
    if (!source.hasLaunchInstructions()) return None

    val configuration = source.parseConfiguration()
    val loginData = source.parseLoginData(standInAllowed = configuration.value != null)

    // A wrong type is reported ahead of anything else: it is the most specific diagnosis available,
    // and every other reader would just see the extra as missing.
    val reason = source.mistypedExtra() ?: configuration.reason ?: loginData.reason
    return if (reason != null) {
        Malformed(reason)
    } else {
        Supplied(
            StoreLaunchRequest(
                configuration = configuration.value,
                loginData = loginData.value,
                // Reaching the chat is the point of launching this way, so it is opted out of rather
                // than into.
                openChat = source.boolean(OPEN_CHAT) ?: true,
            )
        )
    }
}

/**
 * Outcome of reading one part of a launch request.
 *
 * @param Type Type of the part which was read.
 * @property value What was read, which is null when that part wasn't asked for.
 * @property reason Why the part couldn't be read, or null if there was nothing wrong with it.
 */
private class Parsed<out Type> private constructor(val value: Type?, val reason: String?) {
    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        /** A part which was read, or which wasn't asked for when [value] is null. */
        fun <Type> of(value: Type?) = Parsed(value, null)

        /** A part which was asked for but couldn't be read, explained by [reason]. */
        fun invalid(reason: String) = Parsed<Nothing>(null, reason)
    }
}

/** Every string extra the launch path understands. */
private val STRING_KEYS = listOf(
    ENVIRONMENT, ENVIRONMENT_HOST, CHANNEL_ID, FIRST_NAME, LAST_NAME, CUSTOMER_ID,
    ENV_NAME, ENV_LOCATION, BASE_URL, SOCKET_URL, ORIGIN_HEADER, CHAT_URL, TOKEN_URL, LOGGER_URL,
)

/** Extras which only mean something alongside [ENVIRONMENT]. */
private val CHANNEL_KEYS = listOf(
    ENVIRONMENT_HOST, CHANNEL_ID,
    ENV_NAME, ENV_LOCATION, BASE_URL, SOCKET_URL, ORIGIN_HEADER, CHAT_URL, TOKEN_URL, LOGGER_URL,
)

/** Extras which together describe a custom environment url by url. */
private val EXPLICIT_URL_KEYS = CHANNEL_KEYS - ENVIRONMENT_HOST - CHANNEL_ID

/**
 * True if any extra suggests this intent is trying to drive the app rather than just open it.
 *
 * [OPEN_CHAT] counts only when it was actually supplied: it defaults to true, so testing its value
 * would make every ordinary launch look like an automated one.
 */
private fun ExtrasSource.hasLaunchInstructions() =
    STRING_KEYS.any { has(it) } || has(BRAND_ID) || has(OPEN_CHAT)

/**
 * The first extra which was supplied but not in the type it has to be, described for the log.
 *
 * Without this a mistyped extra reads as absent: `--ei brandId 1234` would be ignored entirely, and
 * `--es openChat false` would be read as the `true` default -- the opposite of what was asked for.
 */
private fun ExtrasSource.mistypedExtra(): String? {
    STRING_KEYS.firstOrNull { has(it) && string(it) == null }?.let {
        return "'$it' must be supplied as a string (--es)"
    }
    return when {
        has(BRAND_ID) && long(BRAND_ID) == null -> "'$BRAND_ID' must be supplied as a long (--el)"
        has(OPEN_CHAT) && boolean(OPEN_CHAT) == null -> "'$OPEN_CHAT' must be supplied as a boolean (--ez)"
        else -> null
    }
}

private fun ExtrasSource.parseLoginData(standInAllowed: Boolean): Parsed<LoginData> {
    val firstName = string(FIRST_NAME)
    val lastName = string(LAST_NAME)
    val customerId = string(CUSTOMER_ID)

    return when {
        firstName != null && lastName != null -> Parsed.of(
            LoginData(
                userName = ChatUserName(lastName = lastName, firstName = firstName),
                customerId = customerId,
            )
        )

        firstName != null || lastName != null ->
            Parsed.invalid("'$FIRST_NAME' and '$LAST_NAME' must be supplied together")

        standInAllowed -> Parsed.of(
            LoginData(
                userName = ChatUserName(
                    lastName = DefaultChatUser.LAST_NAME,
                    firstName = DefaultChatUser.FIRST_NAME,
                ),
                customerId = customerId,
            )
        )

        // Without a channel there is no sign-in to attach it to, and quietly dropping it would hide a
        // mistyped command.
        customerId != null -> Parsed.invalid(
            "'$CUSTOMER_ID' also requires '$FIRST_NAME' and '$LAST_NAME', or an '$ENVIRONMENT' to sign in to"
        )

        else -> Parsed.of(null)
    }
}

private fun ExtrasSource.parseConfiguration(): Parsed<SdkConfigurationRequest> {
    val environmentName = string(ENVIRONMENT) ?: return if (namesAChannel()) {
        Parsed.invalid("'$ENVIRONMENT' is required alongside '$BRAND_ID', '$CHANNEL_ID' and the custom environment extras")
    } else {
        Parsed.of(null)
    }
    val parsedEnvironment = parseEnvironment(environmentName)
    val environment = parsedEnvironment.value
    val brandId = long(BRAND_ID)
    val channelId = string(CHANNEL_ID)

    return when {
        environment == null -> Parsed.invalid(parsedEnvironment.reason.orEmpty())
        brandId == null -> Parsed.invalid("'$ENVIRONMENT' also requires '$BRAND_ID' (--el, a long)")
        channelId == null -> Parsed.invalid("'$ENVIRONMENT' also requires '$CHANNEL_ID'")
        else -> Parsed.of(SdkConfigurationRequest(environment, brandId, channelId))
    }
}

/** A bare hostname: letters, digits, dots and dashes, starting and ending on an alphanumeric. */
private val HOST_STEM = Regex("[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]")

/**
 * Read the requested environment.
 *
 * A named one is taken at face value and looked up later, once the bundled definitions have loaded.
 * A custom one is described by the intent itself, either by the host stem its urls all derive from
 * or url by url -- one or the other, never both, since there would be no way to tell which was meant.
 */
private fun ExtrasSource.parseEnvironment(environmentName: String): Parsed<RequestedEnvironment> {
    if (environmentName != StoreEnvironment.CUSTOM) return Parsed.of(RequestedEnvironment.Named(environmentName))

    val host = string(ENVIRONMENT_HOST)
    val hasExplicitUrls = EXPLICIT_URL_KEYS.any { has(it) }

    return when {
        host != null && hasExplicitUrls -> Parsed.invalid(
            "supply either '$ENVIRONMENT_HOST' or the individual environment urls, not both"
        )

        // Every url is built by prefixing this, so anything that isn't a bare host would go unnoticed
        // here and surface much later as a connection failure naming nothing. Matched as a shape
        // rather than as a list of known mistakes, so a path or a mis-quoted space is caught too.
        host != null && !HOST_STEM.matches(host) -> Parsed.invalid(
            "'$ENVIRONMENT_HOST' is a host stem such as 'de-na1.example.com', without scheme, path or trailing slash"
        )

        host != null -> Parsed.of(RequestedEnvironment.Custom(sdkEnvironmentFromHost(host)))

        hasExplicitUrls -> explicitEnvironment()?.let { Parsed.of(RequestedEnvironment.Custom(it)) }
            ?: Parsed.invalid(
                "'$ENVIRONMENT=${StoreEnvironment.CUSTOM}' with individual urls also requires '$ENV_NAME', " +
                        "'$ENV_LOCATION', '$BASE_URL', '$SOCKET_URL', '$ORIGIN_HEADER', '$CHAT_URL' and '$TOKEN_URL'"
            )

        else -> Parsed.invalid(
            "'$ENVIRONMENT=${StoreEnvironment.CUSTOM}' requires either '$ENVIRONMENT_HOST' or the individual urls"
        )
    }
}

/** True if any extra only meaningful alongside [ENVIRONMENT] was supplied. */
private fun ExtrasSource.namesAChannel() = has(BRAND_ID) || CHANNEL_KEYS.any { has(it) }

private fun ExtrasSource.explicitEnvironment(): SdkEnvironment? = SdkEnvironment(
    name = string(ENV_NAME) ?: return null,
    location = string(ENV_LOCATION) ?: return null,
    baseUrl = string(BASE_URL) ?: return null,
    socketUrl = string(SOCKET_URL) ?: return null,
    originHeader = string(ORIGIN_HEADER) ?: return null,
    chatUrl = string(CHAT_URL) ?: return null,
    loggerUrl = string(LOGGER_URL).orEmpty(),
    tokenUrl = string(TOKEN_URL) ?: return null,
)
