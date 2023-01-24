package com.nice.cxonechat.tool

private val Default = ('a'..'z') + ('A'..'Z') + ('0'..'9')

internal fun nextString(length: Int = 10, pool: List<Char> = Default) = buildString {
    repeat(length) {
        append(pool.random())
    }
}

internal fun nextStringPair(length: () -> Int = { 10 }, pool: List<Char> = Default): Pair<String, String> =
    nextString(length(), pool) to nextString(length(), pool)

internal fun nextStringMap(
    capacity: Int = 1,
    length: () -> Int = { 10 },
    pool: List<Char> = Default,
): Map<String, String> = buildMap(capacity = capacity) {
    put(nextString(length(), pool), nextString(length(), pool))
}
