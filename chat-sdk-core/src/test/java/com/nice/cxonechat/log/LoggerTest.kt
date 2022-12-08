package com.nice.cxonechat.log

import com.nice.cxonechat.tool.nextString
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

internal class LoggerTest {

    private lateinit var logger: Logger

    @Before
    fun prepare() {
        logger = mock()
    }

    @Test
    fun finest_withoutThrowable() {
        val message = nextString()
        logger.finest(message)
        verify(logger).log(Level.Finest, message, null)
    }

    @Test
    fun finest_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.finest(message, throwable)
        verify(logger).log(Level.Finest, message, throwable)
    }

    @Test
    fun finer_withoutThrowable() {
        val message = nextString()
        logger.finer(message)
        verify(logger).log(Level.Finer, message, null)
    }

    @Test
    fun finer_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.finer(message, throwable)
        verify(logger).log(Level.Finer, message, throwable)
    }

    @Test
    fun fine_withoutThrowable() {
        val message = nextString()
        logger.fine(message)
        verify(logger).log(Level.Fine, message, null)
    }

    @Test
    fun fine_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.fine(message, throwable)
        verify(logger).log(Level.Fine, message, throwable)
    }

    @Test
    fun info_withoutThrowable() {
        val message = nextString()
        logger.info(message)
        verify(logger).log(Level.Info, message, null)
    }

    @Test
    fun info_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.info(message, throwable)
        verify(logger).log(Level.Info, message, throwable)
    }

    @Test
    fun warning_withoutThrowable() {
        val message = nextString()
        logger.warning(message)
        verify(logger).log(Level.Warning, message, null)
    }

    @Test
    fun warning_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.warning(message, throwable)
        verify(logger).log(Level.Warning, message, throwable)
    }

    @Test
    fun severe_withoutThrowable() {
        val message = nextString()
        logger.severe(message)
        verify(logger).log(Level.Severe, message, null)
    }

    @Test
    fun severe_withThrowable() {
        val message = nextString()
        val throwable = RuntimeException()
        logger.severe(message, throwable)
        verify(logger).log(Level.Severe, message, throwable)
    }

}
