package com.nice.cxonechat.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration

private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'z'", Locale.ROOT).also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}
private val timestampFormatWithoutMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.ROOT).also {
    it.timeZone = TimeZone.getTimeZone("UTC")
}

@Throws(ParseException::class)
internal fun String.timestampToDate(): Date {
    return try {
        timestampFormat.parse(this)
    } catch (ignore: ParseException) {
        timestampFormatWithoutMillis.parse(this)
    }.let(::requireNotNull)
}

internal fun Date.toTimestamp(): String {
    return timestampFormat.format(this)
}

internal fun DateTime.toTimestamp(): String {
    return timestampFormatWithoutMillis.format(date)
}

internal operator fun Date.plus(millis: Long) = Date(time + millis)

internal fun Date.expiresWithin(duration: Duration): Boolean {
    val now = Date() + duration.inWholeMilliseconds
    return now.after(this)
}
