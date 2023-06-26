package com.nice.cxonechat.log

/**
 * Logging level.
 */
sealed class Level {

    /** Integer representation of a given level. */
    abstract val intValue: Int

    /** Compares itself to [other] level for convenience. */
    operator fun compareTo(other: Level): Int = intValue.compareTo(other.intValue)

    /** Severe level corresponds to integer value of 1000. */
    object Severe : Level() {
        override val intValue: Int
            get() = 1000
    }

    /** Warning level corresponds to integer value of 900. */
    object Warning : Level() {
        override val intValue: Int
            get() = 900
    }

    /** Info level corresponds to integer value of 800. */
    object Info : Level() {
        override val intValue: Int
            get() = 800
    }

    /** Config level corresponds to integer value of 700. */
    object Config : Level() {
        override val intValue: Int
            get() = 700
    }

    /** Fine level corresponds to integer value of 500. */
    object Fine : Level() {
        override val intValue: Int
            get() = 500
    }

    /** Finer level corresponds to integer value of 400. */
    object Finer : Level() {
        override val intValue: Int
            get() = 400
    }

    /** Finest level corresponds to integer value of 300. */
    object Finest : Level() {
        override val intValue: Int
            get() = 300
    }

    /**
     * All level corresponds to integer value of [Int.MIN_VALUE].
     * Note that this option may not be supported by all Logger
     * implementations.
     * */
    object All : Level() {
        override val intValue: Int
            get() = Int.MIN_VALUE
    }

    /**
     * Custom level allows you to specify your own values and
     * store them statically.
     * */
    class Custom(
        override val intValue: Int,
    ) : Level()
}
